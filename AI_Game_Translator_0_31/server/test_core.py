import os
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from core import ApiError, Ledger, Service, entitlement, account_hash, SUBS, LIFETIME, LIMIT

class FakePlay:
    def __init__(self, data, fail_ack=False):
        self.data, self.fail_ack, self.acks = data, fail_ack, []
    def get(self, product, token):
        return self.data.get(token)
    def acknowledge(self, product, token):
        if self.fail_ack:
            raise ApiError(503, 'acknowledgement_failed')
        self.acks.append((product, token))

class LedgerTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.path = os.path.join(self.directory.name, 'ledger.db')
        self.ledger = Ledger(self.path)
    def tearDown(self):
        self.directory.cleanup()
    def test_reinstall_keeps_same_account_usage(self):
        account = account_hash('google-subject-123')
        self.ledger.consume(account, 'session-before-install', 61000)
        self.assertEqual(61000, Ledger(self.path).used(account))
        self.assertEqual(92000, self.ledger.consume(account, 'session-after-install', 31000))
    def test_retry_and_out_of_order_do_not_double_charge(self):
        self.ledger.consume('a', 'session-retry-001', 12000)
        self.ledger.consume('a', 'session-retry-001', 12000)
        self.ledger.consume('a', 'session-retry-001', 8000)
        self.assertEqual(12000, self.ledger.used('a'))
    def test_parallel_devices_share_account_limit(self):
        with ThreadPoolExecutor(max_workers=4) as pool:
            list(pool.map(lambda n: self.ledger.consume('a', f'session-device-{n:03}', 60000), range(4)))
        self.assertEqual(LIMIT, self.ledger.used('a'))
    def test_session_cannot_be_claimed_by_another_account(self):
        self.ledger.consume('a', 'session-owner-001', 1000)
        with self.assertRaises(ApiError):
            self.ledger.consume('b', 'session-owner-001', 2000)
        self.assertEqual(0, self.ledger.used('b'))
    def test_invalid_values_fail_closed(self):
        for value in (-1, 180001, True, '1000', None):
            with self.assertRaises(ApiError):
                self.ledger.consume('a', 'session-invalid-01', value)
        self.assertEqual(0, self.ledger.used('a'))

class EntitlementTests(unittest.TestCase):
    def setUp(self):
        self.account = account_hash('google-subject-123')
        self.now = 1700000000
    def subscription(self, state='SUBSCRIPTION_STATE_ACTIVE', expiry='2099-01-01T00:00:00Z'):
        return {'subscriptionState': state, 'acknowledgementState': 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
                'externalAccountIdentifiers': {'obfuscatedExternalAccountId': self.account},
                'lineItems': [{'productId': SUBS, 'expiryTime': expiry, 'offerDetails': {'basePlanId': 'monthly'}}]}
    def lifetime(self, state=0):
        return {'purchaseState': state, 'consumptionState': 0, 'acknowledgementState': 1,
                'obfuscatedExternalAccountId': self.account}
    def test_active_grace_and_canceled_paid_period_grant(self):
        for state in ('SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', 'SUBSCRIPTION_STATE_CANCELED'):
            granted, until = entitlement(SUBS, self.subscription(state), self.account, self.now)
            self.assertTrue(granted)
            self.assertEqual(self.now+300, until)
    def test_pending_hold_pause_expired_never_grant(self):
        for state in ('SUBSCRIPTION_STATE_PENDING', 'SUBSCRIPTION_STATE_ON_HOLD', 'SUBSCRIPTION_STATE_PAUSED', 'SUBSCRIPTION_STATE_EXPIRED', ''):
            self.assertFalse(entitlement(SUBS, self.subscription(state), self.account, self.now)[0])
        self.assertFalse(entitlement(SUBS, self.subscription(expiry='2020-01-01T00:00:00Z'), self.account, self.now)[0])
    def test_unknown_product_or_plan_does_not_grant(self):
        data = self.subscription();data['lineItems'][0]['offerDetails']['basePlanId'] = 'unknown'
        self.assertFalse(entitlement(SUBS, data, self.account, self.now)[0])
        with self.assertRaises(ApiError):
            entitlement('unknown', {}, self.account, self.now)
    def test_lifetime_pending_refunded_consumed_do_not_grant(self):
        self.assertTrue(entitlement(LIFETIME, self.lifetime(), self.account, self.now)[0])
        for state in (1, 2, None):
            self.assertFalse(entitlement(LIFETIME, self.lifetime(state), self.account, self.now)[0])
        data=self.lifetime();data['consumptionState']=1
        self.assertFalse(entitlement(LIFETIME, data, self.account, self.now)[0])
    def test_other_account_cannot_restore_a_token(self):
        for product,data in ((SUBS,self.subscription()), (LIFETIME,self.lifetime())):
            with self.assertRaises(ApiError):
                entitlement(product, data, 'other', self.now)
    def test_expiry_limits_lease(self):
        data=self.subscription(expiry='2023-11-14T22:13:40Z')
        self.assertEqual(self.now+20, entitlement(SUBS,data,self.account,self.now)[1])
    def test_ack_failure_never_grants_and_no_purchase_never_grants(self):
        with tempfile.TemporaryDirectory() as directory:
            ledger=Ledger(os.path.join(directory, 'ledger.db'))
            data=self.lifetime();data['acknowledgementState']=0
            service=Service(ledger,FakePlay({'token':data},fail_ack=True),lambda:self.now)
            with self.assertRaises(ApiError):
                service.state(self.account,[{'product':LIFETIME,'token':'token'}])
            self.assertFalse(service.state(self.account,[])['pro'])
            self.assertFalse(Service(ledger,FakePlay({}),lambda:self.now).state(self.account,[{'product':LIFETIME,'token':'missing'}])['pro'])
    def test_unacknowledged_valid_purchase_is_acknowledged(self):
        with tempfile.TemporaryDirectory() as directory:
            play=FakePlay({'token':dict(self.lifetime(),acknowledgementState=0)})
            state=Service(Ledger(os.path.join(directory,'db')),play,lambda:self.now).state(self.account,[{'product':LIFETIME,'token':'token'}])
            self.assertTrue(state['pro']);self.assertEqual([(LIFETIME,'token')],play.acks)

if __name__ == '__main__':
    unittest.main()
