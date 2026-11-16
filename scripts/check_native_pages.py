"""Read actual packaged ELF program headers; never infer compatibility from version names."""
import argparse
import json
import struct
import zipfile
from pathlib import Path

PAGE = 16384
ABI64 = {'arm64-v8a', 'x86_64'}

def inspect_elf(data):
    if len(data) < 64 or data[:4] != b'\x7fELF':
        raise ValueError('Invalid ELF header')
    endian = {1: '<', 2: '>'}.get(data[5])
    if endian is None:
        raise ValueError('Invalid ELF byte order')
    if data[4] == 2:
        offset = struct.unpack_from(endian+'Q', data, 32)[0]
        size, count = struct.unpack_from(endian+'HH', data, 54)
        fmt = endian+'IIQQQQQQ'; indexes = (0, 2, 3, 7)
    elif data[4] == 1:
        offset = struct.unpack_from(endian+'I', data, 28)[0]
        size, count = struct.unpack_from(endian+'HH', data, 42)
        fmt = endian+'IIIIIIII'; indexes = (0, 1, 2, 7)
    else:
        raise ValueError('Invalid ELF class')
    if size < struct.calcsize(fmt) or offset+size*count > len(data):
        raise ValueError('Truncated program headers')
    loads = []
    for i in range(count):
        row = struct.unpack_from(fmt, data, offset+i*size)
        kind, start, virtual, alignment = (row[n] for n in indexes)
        if kind == 1:
            loads.append({'offset': start, 'virtualAddress': virtual, 'alignment': alignment,
                          'compatible': alignment >= PAGE and (virtual-start) % PAGE == 0})
    if not loads:
        raise ValueError('No PT_LOAD segments')
    return loads

def inspect_archive(path):
    results = []; failures = []
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            parts = name.split('/')
            if not name.endswith('.so') or 'lib' not in parts:
                continue
            index = parts.index('lib')
            abi = parts[index+1]
            try:
                segments = inspect_elf(archive.read(name))
                ok = all(row['compatible'] for row in segments)
                results.append({'library': name, 'abi': abi, 'compatible': ok, 'segments': segments})
                if abi in ABI64 and not ok:
                    failures.append(name)
            except (ValueError, struct.error) as error:
                results.append({'library': name, 'abi': abi, 'error': str(error)})
                failures.append(name)
    if results and not any(row['abi'] in ABI64 for row in results):
        failures.append('Native code exists but no 64-bit library was packaged')
    return {'archive': str(path), 'check': 'ELF PT_LOAD alignment for 64-bit libraries',
            'passed': not failures, 'failures': failures, 'libraries': results}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('archive')
    parser.add_argument('--report', required=True)
    args = parser.parse_args()
    try:
        report = inspect_archive(args.archive)
    except (OSError, zipfile.BadZipFile) as error:
        report = {
            'archive': args.archive, 'passed': False, 'libraries': [],
            'check': 'Archive availability before ELF alignment',
            'failures': [str(error)],
            'error': 'AAB/APK could not be opened. Check the preceding Gradle build log; alignment was not checked.'
        }
    output = Path(args.report)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2), encoding='utf-8')
    if 'error' in report:
        print(report['error'])
        for failure in report['failures']:
            print(failure)
    else:
        print(f"{len(report['libraries'])} libraries checked; ELF alignment: {'PASS' if report['passed'] else 'FAIL'}")
        for name in report['failures']:
            print(f'16 KB incompatibility: {name}')
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
