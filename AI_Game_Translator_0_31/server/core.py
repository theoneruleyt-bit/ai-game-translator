"""Account-bound trial ledger and Play entitlement decisions. No client 'pro' flag accepted."""
import hashlib
from contextlib import contextmanager
import sqlite3
import time
from datetime import datetime

LIMIT = 180_000
PACKAGE = 'com.optima.gametranslator'
SUBS = 'translator_pro'
LIFETIME = 'translator_pro_lifetime'

class ApiError(Exception):
    def __init__(self, status, code):
        self.status, self.code = status, code
        super().__init__(code)

def account_hash(subject):
    return hashlib.sha256(subject.encode('utf-8')).hexdigest()

def expiry_seconds(value):
    try:
        return datetime.fromisoformat(value.replace('Z', '+00:00')).timestamp()
    except (ValueError, TypeError, AttributeError):
        return 0

def entitlement(product, data, account, now):
    if product == LIFETIME:
        if data.get('obfuscatedExternalAccountId') != account:
            raise ApiError(403, 'purchase_account_mismatch')
        return (data.get('purchaseState') == 0 and data.get('consumptionState') == 0), now + 300
    if product == SUBS:
        if data.get('externalAccountIdentifiers', {}).get('obfuscatedExternalAccountId') != account:
            raise ApiError(403, 'purchase_account_mismatch')
        states = {'SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', 'SUBSCRIPTION_STATE_CANCELED'}
        items = [item for item in data.get('lineItems', []) if item.get('productId') == SUBS
                 and item.get('offerDetails', {}).get('basePlanId') in {'monthly', 'yearly'}]
        ends = max((expiry_seconds(i.get('expiryTime')) for i in items), default=0)
        return data.get('subscriptionState') in states and ends > now, min(ends, now + 300)
    raise ApiError(400, 'unknown_product')

class Ledger:
    def __init__(self, path):
        self.path = path
        with self.db() as con:
            con.executescript('''
                CREATE TABLE IF NOT EXISTS accounts (id TEXT PRIMARY KEY, used INTEGER NOT NULL DEFAULT 0);
                CREATE TABLE IF NOT EXISTS sessions (id TEXT PRIMARY KEY, account TEXT NOT NULL, elapsed INTEGER NOT NULL DEFAULT 0);
            ''')
    @contextmanager
    def db(self):
        con = sqlite3.connect(self.path, timeout=10)
        try:
            with con:
                yield con
        finally:
            con.close()
    def used(self, account):
        with self.db() as con:
            row = con.execute('SELECT used FROM accounts WHERE id=?', (account,)).fetchone()
            return row[0] if row else 0
    def consume(self, account, session, elapsed):
        # Cumulative per-session values make retries and out-of-order delivery idempotent.
        if not isinstance(session, str) or not (16 <= len(session) <= 64) or not all(c.isalnum() or c == '-' for c in session):
            raise ApiError(400, 'invalid_session')
        if type(elapsed) is not int or not 0 <= elapsed <= LIMIT:
            raise ApiError(400, 'invalid_elapsed')
        with self.db() as con:
            con.execute('BEGIN IMMEDIATE')
            con.execute('INSERT OR IGNORE INTO accounts(id) VALUES (?)', (account,))
            row = con.execute('SELECT account,elapsed FROM sessions WHERE id=?', (session,)).fetchone()
            if row and row[0] != account:
                raise ApiError(403, 'session_account_mismatch')
            previous = row[1] if row else 0
            value = max(previous, elapsed)
            con.execute('INSERT INTO sessions VALUES (?,?,?) ON CONFLICT(id) DO UPDATE SET elapsed=excluded.elapsed', (session, account, value))
            con.execute('UPDATE accounts SET used=MIN(?,used+?) WHERE id=?', (LIMIT, value-previous, account))
            return con.execute('SELECT used FROM accounts WHERE id=?', (account,)).fetchone()[0]

class Service:
    def __init__(self, ledger, play, clock=time.time):
        self.ledger, self.play, self.clock = ledger, play, clock
    def state(self, account, purchases):
        if not isinstance(purchases, list) or len(purchases) > 16:
            raise ApiError(400, 'invalid_purchases')
        now = self.clock()
        valid_until = now
        for item in purchases:
            if not isinstance(item, dict):
                raise ApiError(400, 'invalid_purchase')
            product, token = item.get('product'), item.get('token')
            if product not in {SUBS, LIFETIME} or not isinstance(token, str) or not 1 <= len(token) <= 4096:
                raise ApiError(400, 'invalid_purchase')
            data = self.play.get(product, token)
            if data is None:  # Google 404/410 means no current entitlement, never a positive result.
                continue
            granted, until = entitlement(product, data, account, now)
            if granted:
                acknowledged = data.get('acknowledgementState') == (1 if product == LIFETIME else 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED')
                if not acknowledged:
                    self.play.acknowledge(product, token)  # Failure propagates; never unlock.
                valid_until = max(valid_until, until)
        return {'pro': valid_until > now, 'validForMillis': max(0, int((valid_until-now)*1000)),
                'trialUsedMillis': self.ledger.used(account)}
