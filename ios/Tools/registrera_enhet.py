#!/usr/bin/env python3
"""Registrerar en iPhone hos Apple via App Store Connect-API:t.

Körs i flödet ios-adhoc.yml före arkiveringen, så att den ad hoc-profil
som Xcode skapar med -allowProvisioningUpdates får med enheten.

Skriptet är med flit beroende av ingenting utöver Pythons standardbibliotek
och openssl, som finns på varje macOS-byggare. En pip-installation till är
ett steg till som kan falla.

Miljövariabler:
  KEY_ID      nyckelns id, t.ex. 2X9ABC3DEF
  ISSUER_ID   utgivarens UUID
  KEY_PATH    sökväg till AuthKey_<KEY_ID>.p8
  UDID        enhetens udid, 25 eller 40 tecken
  DEVICE_NAME namnet enheten får i Apples lista

Avslutar med 0 när enheten finns och är påslagen, annars med 1.
"""

import base64
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.appstoreconnect.apple.com/v1"


def need(name):
    value = os.environ.get(name, "").strip()
    if not value:
        sys.exit("FEL: miljövariabeln %s är tom." % name)
    return value


def b64(raw):
    return base64.urlsafe_b64encode(raw).rstrip(b"=")


def der_to_raw(der):
    """ECDSA-signaturen från openssl är DER. JWT vill ha r och s råa."""
    if not der or der[0] != 0x30:
        sys.exit("FEL: openssl gav ingen DER-signatur.")
    i = 2
    if der[1] & 0x80:                      # lång längdform
        i = 2 + (der[1] & 0x7F)
    numbers = []
    for _ in range(2):
        if der[i] != 0x02:
            sys.exit("FEL: oväntad DER-struktur i signaturen.")
        length = der[i + 1]
        value = der[i + 2:i + 2 + length]
        i += 2 + length
        numbers.append(value.lstrip(b"\x00").rjust(32, b"\x00"))
    return numbers[0] + numbers[1]


def token(key_id, issuer_id, key_path):
    now = int(time.time())
    head = {"alg": "ES256", "kid": key_id, "typ": "JWT"}
    body = {
        "iss": issuer_id,
        "iat": now,
        "exp": now + 900,                  # Apple tillåter högst 20 minuter
        "aud": "appstoreconnect-v1",
    }
    def part(obj):
        return b64(json.dumps(obj, separators=(",", ":")).encode())
    signed = part(head) + b"." + part(body)
    done = subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", key_path],
        input=signed, capture_output=True)
    if done.returncode != 0:
        sys.exit("FEL: openssl kunde inte signera: %s"
                 % done.stderr.decode(errors="replace").strip())
    return (signed + b"." + b64(der_to_raw(done.stdout))).decode()


def call(jwt, method, path, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    request = urllib.request.Request(API + path, data=data, method=method)
    request.add_header("Authorization", "Bearer " + jwt)
    request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, timeout=60) as answer:
            text = answer.read().decode(errors="replace")
            return answer.status, (json.loads(text) if text else {})
    except urllib.error.HTTPError as problem:
        text = problem.read().decode(errors="replace")
        try:
            return problem.code, json.loads(text)
        except ValueError:
            return problem.code, {"raw": text}
    except urllib.error.URLError as problem:
        sys.exit("FEL: nådde inte Apple: %s" % problem.reason)


def errors(answer):
    found = answer.get("errors") or []
    return "; ".join(
        "%s: %s" % (one.get("title", "?"), one.get("detail", "")) for one in found
    ) or json.dumps(answer)[:400]


def main():
    key_id = need("KEY_ID")
    issuer_id = need("ISSUER_ID")
    key_path = need("KEY_PATH")
    udid = need("UDID")
    name = need("DEVICE_NAME")

    if not os.path.isfile(key_path):
        sys.exit("FEL: hittar ingen nyckel på %s." % key_path)

    jwt = token(key_id, issuer_id, key_path)

    # Finns enheten redan? Apple svarar med fel när samma udid läggs in två
    # gånger, så en fråga först gör loggen läsbar i stället för röd.
    status, answer = call(
        jwt, "GET",
        "/devices?" + urllib.parse.urlencode({"filter[udid]": udid, "limit": 200}))
    if status != 200:
        sys.exit("FEL: kunde inte läsa enhetslistan (%s). %s"
                 % (status, errors(answer)))

    hit = None
    for one in answer.get("data", []):
        if (one.get("attributes", {}).get("udid") or "").lower() == udid.lower():
            hit = one
            break

    if hit is None:
        status, answer = call(jwt, "POST", "/devices", {
            "data": {
                "type": "devices",
                "attributes": {"name": name, "platform": "IOS", "udid": udid},
            }
        })
        if status in (200, 201):
            print("Enheten registrerades: %s (%s)" % (name, udid))
            return 0
        # 409 betyder oftast att enheten redan finns, men var avstängd eller
        # tillhör en annan plattform. Då duger inte en tyst fortsättning.
        print("FEL: Apple tog inte emot enheten (%s). %s" % (status, errors(answer)))
        return 1

    state = hit.get("attributes", {}).get("status", "?")
    print("Enheten fanns redan: %s (%s), läge %s"
          % (hit.get("attributes", {}).get("name", name), udid, state))
    if state == "ENABLED":
        return 0

    # En avstängd enhet räknas inte in i profilen, och då går appen inte att
    # installera. Apple tillåter att läget slås på igen.
    status, answer = call(jwt, "PATCH", "/devices/" + hit["id"], {
        "data": {"type": "devices", "id": hit["id"],
                 "attributes": {"status": "ENABLED"}}
    })
    if status == 200:
        print("Enheten slogs på igen.")
        return 0
    print("FEL: enheten är avstängd och kunde inte slås på (%s). %s"
          % (status, errors(answer)))
    return 1


if __name__ == "__main__":
    sys.exit(main())
