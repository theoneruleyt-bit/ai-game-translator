#!/usr/bin/env python3
"""Verify real optimized AAB contents, mapping and preserved JNI entry classes."""
import argparse
import json
import pathlib
import re
import struct
import zipfile

REQUIRED_JNI = (
    'Lcom/k2fsa/sherpa/onnx/OnlineRecognizer;',
    'Lcom/k2fsa/sherpa/onnx/OnlinePunctuation;',
    'Lcom/k2fsa/sherpa/onnx/Vad;',
    'Lcom/google/ai/edge/litertlm/Engine;',
    'Lorg/tensorflow/lite/Interpreter;',
    'Lorg/vosk/Recognizer;',
    'Lcom/sun/jna/Native;',
)


def dex_classes(data):
    if not data.startswith(b'dex\n'):
        raise ValueError('Expected standard DEX')
    u32 = lambda at: struct.unpack_from('<I', data, at)[0]
    strings_size, strings_at = u32(56), u32(60)
    types_size, types_at = u32(64), u32(68)
    classes_size, classes_at = u32(96), u32(100)
    if strings_at + strings_size * 4 > len(data) or types_at + types_size * 4 > len(data) or classes_at + classes_size * 32 > len(data):
        raise ValueError('Invalid DEX tables')
    result = set()
    for index in range(classes_size):
        type_id = u32(classes_at + index * 32)
        if type_id >= types_size:
            raise ValueError('Invalid DEX type')
        string_id = u32(types_at + type_id * 4)
        if string_id >= strings_size:
            raise ValueError('Invalid DEX string')
        at = u32(strings_at + string_id * 4)
        for _ in range(5):
            value = data[at]
            at += 1
            if value < 128:
                break
        else:
            raise ValueError('Invalid DEX string length')
        end = data.index(0, at)
        result.add(data[at:end].decode('utf-8'))
    return result


def inspect(aab, mapping_path):
    mapping = pathlib.Path(mapping_path).read_text(encoding='utf-8')
    renamed = []
    for line in mapping.splitlines():
        match = re.fullmatch(r'(com\.optima\.gametranslator\.[^ ]+) -> ([^:]+):', line)
        if match and match[1] != match[2]:
            renamed.append(match[1])
    if not renamed:
        raise ValueError('No renamed application classes: release optimization missing')
    with zipfile.ZipFile(aab) as archive:
        embedded = 'BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map'
        if archive.read(embedded).decode('utf-8') != mapping:
            raise ValueError('Embedded mapping differs from this release mapping')
        classes = set()
        dex_bytes = 0
        for name in archive.namelist():
            if re.fullmatch(r'base/dex/classes\d*\.dex', name):
                data = archive.read(name)
                dex_bytes += len(data)
                classes.update(dex_classes(data))
        missing = sorted(set(REQUIRED_JNI) - classes)
        if missing:
            raise ValueError('Native binding classes removed or renamed: ' + ', '.join(missing))
        native_symbols = [name for name in archive.namelist() if name.startswith('BUNDLE-METADATA/com.android.tools.build.debugsymbols/')]
    return {'ok': True, 'renamed_app_classes': len(renamed), 'dex_classes': len(classes), 'dex_bytes': dex_bytes,
            'mapping_embedded': True, 'required_jni_preserved': list(REQUIRED_JNI),
            'native_symbol_entries': len(native_symbols),
            'note': 'Pre-stripped vendor libraries cannot regain missing native symbols. Play optimization percentage must be checked after upload.'}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('aab')
    parser.add_argument('--mapping', default='app/build/outputs/mapping/release/mapping.txt')
    parser.add_argument('--report', default='app/build/reports/release-optimization.json')
    args = parser.parse_args()
    report = inspect(args.aab, args.mapping)
    target = pathlib.Path(args.report)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
