#!/usr/bin/env python3
"""Generates deterministic msw 2.x mock handlers from an OpenAPI 3.1 spec.

Usage: gen_msw_handlers.py <spec.yaml> <output.ts>
"""
import re
import sys
from pathlib import Path

import yaml

METHOD_ORDER = ["get", "put", "post", "delete", "options", "head", "patch", "trace"]
SUCCESS_ORDER = ["200", "201", "202", "203", "206", "204"]
PARAM_RE = re.compile(r"\{([^}]+)\}")


def resolve(ref, spec):
    node = spec
    for part in ref.lstrip("#/").split("/"):
        node = node[part]
    return node


def example_for_schema(schema, spec, depth=0, seen=frozenset()):
    if schema is None or depth > 30:
        return None
    if "$ref" in schema:
        ref = schema["$ref"]
        if ref in seen:
            return None
        return example_for_schema(resolve(ref, spec), spec, depth + 1, seen | {ref})

    if "const" in schema:
        return schema["const"]
    if "enum" in schema:
        return schema["enum"][0]
    if isinstance(schema.get("examples"), list) and schema["examples"]:
        return schema["examples"][0]
    if "example" in schema:
        return schema["example"]

    if schema.get("allOf"):
        merged = {}
        for sub in schema["allOf"]:
            val = example_for_schema(sub, spec, depth + 1, seen)
            if isinstance(val, dict):
                merged.update(val)
        return merged
    for combiner in ("oneOf", "anyOf"):
        if schema.get(combiner):
            return example_for_schema(schema[combiner][0], spec, depth + 1, seen)

    types = schema.get("type")
    if isinstance(types, list):
        non_null = [t for t in types if t != "null"]
        t = non_null[0] if non_null else "null"
    else:
        t = types

    if t == "object" or (t is None and "properties" in schema):
        props = schema.get("properties", {})
        return {k: example_for_schema(props[k], spec, depth + 1, seen) for k in sorted(props)}
    if t == "array":
        return [example_for_schema(schema.get("items", {}), spec, depth + 1, seen)]
    if t == "string":
        fmt = schema.get("format")
        return {
            "date-time": "2026-09-22T00:00:00Z",
            "date": "2026-09-22",
            "uri": "https://example.com/stub",
            "email": "stub@example.com",
        }.get(fmt, "stub")
    if t == "integer":
        return 1000 if schema.get("format") == "int64" else 0
    if t == "number":
        return 0
    if t == "boolean":
        return False
    return None


def success_response(operation, spec):
    responses = operation.get("responses", {})
    for code in SUCCESS_ORDER:
        if code in responses:
            return code, responses[code]
    return None, None


def response_body(response, spec):
    if response is None:
        return None
    content = response.get("content", {})
    for media_type in ("application/json", "application/problem+json"):
        if media_type in content:
            return example_for_schema(content[media_type].get("schema"), spec)
    return None


def to_msw_path(openapi_path):
    return PARAM_RE.sub(lambda m: ":" + m.group(1), openapi_path)


def json_literal(value, indent=2):
    import json

    return json.dumps(value, indent=indent, sort_keys=True)


def build_handlers(spec):
    handlers = []
    paths = spec.get("paths", {})
    for path in sorted(paths):
        item = paths[path]
        for method in METHOD_ORDER:
            if method not in item:
                continue
            operation = item[method]
            code, response = success_response(operation, spec)
            msw_path = to_msw_path(path)
            if code is None:
                handlers.append(
                    f"  http.{method}('*{msw_path}', () => {{\n"
                    f"    return new HttpResponse(null, {{ status: 501 }});\n"
                    f"  }}),"
                )
                continue
            status = int(code)
            if status == 204 or response_body(response, spec) is None:
                handlers.append(
                    f"  http.{method}('*{msw_path}', () => {{\n"
                    f"    return new HttpResponse(null, {{ status: {status} }});\n"
                    f"  }}),"
                )
                continue
            body = json_literal(response_body(response, spec))
            body_indented = "\n".join(
                "    " + line if line else line for line in body.splitlines()
            ).lstrip()
            handlers.append(
                f"  http.{method}('*{msw_path}', () => {{\n"
                f"    return HttpResponse.json({body_indented}, {{ status: {status} }});\n"
                f"  }}),"
            )
    return handlers


def main():
    if len(sys.argv) != 3:
        print("usage: gen_msw_handlers.py <spec.yaml> <output.ts>", file=sys.stderr)
        sys.exit(1)
    spec_path = Path(sys.argv[1])
    out_path = Path(sys.argv[2])
    spec = yaml.safe_load(spec_path.read_text())
    handlers = build_handlers(spec)

    rel_spec = spec_path.as_posix()
    out = (
        f"// GENERATED by scripts/gen_msw_handlers.py from {rel_spec}; do not edit by hand.\n"
        "import { http, HttpResponse } from 'msw';\n"
        "\n"
        "export const handlers = [\n"
        + "\n".join(handlers)
        + "\n];\n"
    )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(out)
    print(f"{len(handlers)} msw handlers generated -> {out_path}")


if __name__ == "__main__":
    main()
