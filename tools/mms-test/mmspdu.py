"""
Minimal MMS (OMA-MMS-ENC) PDU encoding and decoding, enough to play an MMSC in tests.

Only what the fake MMSC needs: M-Notification.ind, M-Retrieve.conf and M-Send.conf, plus reading
the headers we care about from an M-Send.req.
"""

import struct
import time

# Header field names (well-known, with the high bit set)
BCC = 0x81
CC = 0x82
CONTENT_LOCATION = 0x83
CONTENT_TYPE = 0x84
DATE = 0x85
EXPIRY = 0x88
FROM = 0x89
MESSAGE_CLASS = 0x8A
MESSAGE_ID = 0x8B
MESSAGE_TYPE = 0x8C
MMS_VERSION = 0x8D
MESSAGE_SIZE = 0x8E
RESPONSE_STATUS = 0x92
SUBJECT = 0x96
TO = 0x97
TRANSACTION_ID = 0x98
RETRIEVE_STATUS = 0x99

# Message types
SEND_REQ = 0x80
SEND_CONF = 0x81
NOTIFICATION_IND = 0x82
NOTIFYRESP_IND = 0x83
RETRIEVE_CONF = 0x84
ACKNOWLEDGE_IND = 0x85

# Response / retrieve statuses
RESPONSE_OK = 0x80
RESPONSE_TRANSIENT_FAILURE = 0xC0
RESPONSE_TRANSIENT_NETWORK_PROBLEM = 0xC3
RESPONSE_PERMANENT_FAILURE = 0xE0
RESPONSE_PERMANENT_SERVICE_DENIED = 0xE1
RETRIEVE_OK = 0x80
RETRIEVE_TRANSIENT_FAILURE = 0xC0
RETRIEVE_PERMANENT_MESSAGE_NOT_FOUND = 0xE2

MMS_VERSION_1_2 = 0x92
CT_MULTIPART_MIXED = 0xA3
CT_MULTIPART_RELATED = 0xB3

def text_string(value: str) -> bytes:
    data = value.encode("utf-8")
    if data and data[0] >= 0x80:
        data = b"\x7f" + data  # quote, so it isn't read as a token
    return data + b"\x00"

def uintvar(value: int) -> bytes:
    out = [value & 0x7F]
    value >>= 7
    while value:
        out.insert(0, (value & 0x7F) | 0x80)
        value >>= 7
    return bytes(out)

def value_length(length: int) -> bytes:
    return bytes([length]) if length <= 30 else b"\x1f" + uintvar(length)

def long_integer(value: int) -> bytes:
    data = value.to_bytes(max(1, (value.bit_length() + 7) // 8), "big")
    return bytes([len(data)]) + data

def encoded_address(address: str) -> bytes:
    if "@" not in address and "/TYPE=" not in address:
        address += "/TYPE=PLMN"
    return text_string(address)

def notification_ind(transaction_id: str, sender: str, content_location: str,
                     size: int = 1024, expiry_seconds: int = 7 * 24 * 3600) -> bytes:
    sender_bytes = b"\x80" + encoded_address(sender)  # address-present-token
    expiry = b"\x81" + long_integer(expiry_seconds)  # relative-token
    return (
        bytes([MESSAGE_TYPE, NOTIFICATION_IND])
        + bytes([TRANSACTION_ID]) + text_string(transaction_id)
        + bytes([MMS_VERSION, MMS_VERSION_1_2])
        + bytes([FROM]) + value_length(len(sender_bytes)) + sender_bytes
        + bytes([MESSAGE_CLASS, 0x80])  # personal
        + bytes([MESSAGE_SIZE]) + long_integer(size)
        + bytes([EXPIRY]) + value_length(len(expiry)) + expiry
        + bytes([CONTENT_LOCATION]) + text_string(content_location)
    )

def part(content_type: str, name: str, data: bytes, charset_utf8: bool = False) -> bytes:
    ct = text_string(content_type)
    if charset_utf8:
        ct = value_length(len(ct) + 2) + ct + b"\x81\xea"  # charset = utf-8 (106)
    headers = ct + b"\x8e" + text_string(name)  # content-location
    return uintvar(len(headers)) + uintvar(len(data)) + headers + data

def retrieve_conf(transaction_id: str | None, sender: str, recipient: str, text: str,
                  image: bytes | None = None, status: int | None = None) -> bytes:
    sender_bytes = b"\x80" + encoded_address(sender)
    header = bytes([MESSAGE_TYPE, RETRIEVE_CONF])
    if transaction_id:
        header += bytes([TRANSACTION_ID]) + text_string(transaction_id)
    header += bytes([MMS_VERSION, MMS_VERSION_1_2])
    header += bytes([DATE]) + long_integer(int(time.time()))
    header += bytes([FROM]) + value_length(len(sender_bytes)) + sender_bytes
    header += bytes([TO]) + encoded_address(recipient)
    if status is not None:
        header += bytes([RETRIEVE_STATUS, status])
    header += bytes([CONTENT_TYPE, CT_MULTIPART_MIXED])

    parts = [part("text/plain", "text_0.txt", text.encode("utf-8"), charset_utf8=True)]
    if image:
        parts.append(part("image/png", "image_0.png", image))
    return header + uintvar(len(parts)) + b"".join(parts)

def send_conf(transaction_id: str, status: int, message_id: str | None) -> bytes:
    out = (
        bytes([MESSAGE_TYPE, SEND_CONF])
        + bytes([TRANSACTION_ID]) + text_string(transaction_id)
        + bytes([MMS_VERSION, MMS_VERSION_1_2])
        + bytes([RESPONSE_STATUS, status])
    )
    if message_id and status == RESPONSE_OK:
        out += bytes([MESSAGE_ID]) + text_string(message_id)
    return out

def read_headers(pdu: bytes) -> dict:
    """Reads the simple headers we log or answer to: message type, transaction id, recipients"""
    headers, i = {}, 0
    while i < len(pdu):
        field = pdu[i]
        i += 1
        if field == CONTENT_TYPE:
            break
        if field in (TRANSACTION_ID, SUBJECT, TO, CC, BCC, CONTENT_LOCATION, MESSAGE_ID):
            end = pdu.index(0, i)
            value = pdu[i:end].lstrip(b"\x7f").decode("utf-8", "replace")
            headers.setdefault(field, []).append(value)
            i = end + 1
        elif field in (MESSAGE_TYPE, MMS_VERSION, MESSAGE_CLASS, RESPONSE_STATUS, RETRIEVE_STATUS,
                       0x86, 0x8F, 0x90, 0x94, 0x95, 0x91):
            headers[field] = pdu[i]
            i += 1
        elif field == DATE or field == MESSAGE_SIZE:
            i += 1 + pdu[i]
        else:
            length = pdu[i]
            if length == 0x1F:
                i += 1
                length = 0
                while pdu[i] & 0x80:
                    length = (length << 7) | (pdu[i] & 0x7F)
                    i += 1
                length = (length << 7) | pdu[i]
                i += 1 + length
            elif length < 0x1F:
                i += 1 + length
            elif length >= 0x80:
                i += 1
            else:
                i = pdu.index(0, i) + 1
    return headers

def wap_push(pdu: bytes, transaction: int = 1) -> bytes:
    headers = bytes([0xBE, 0xAF, 0x84])
    return bytes([transaction, 0x06]) + uintvar(len(headers)) + headers + pdu

def _semi_octets(digits: str) -> bytes:
    if len(digits) % 2:
        digits += "F"
    return bytes(int(digits[i + 1] + digits[i], 16) for i in range(0, len(digits), 2))

def sms_deliver_pdus(sender: str, user_data: bytes, reference: int = 1) -> list[bytes]:
    digits = sender.lstrip("+")
    address = bytes([len(digits), 0x91]) + _semi_octets(digits)
    timestamp = _semi_octets(time.strftime("%y%m%d%H%M%S", time.gmtime())) + b"\x00"
    ports = bytes([0x05, 0x04, 0x0B, 0x84, 0x23, 0xF0])

    max_single = 140 - 1 - len(ports)
    if len(user_data) <= max_single:
        chunks = [user_data]
    else:
        size = 140 - 1 - len(ports) - 5
        chunks = [user_data[i:i + size] for i in range(0, len(user_data), size)]

    pdus = []
    for index, chunk in enumerate(chunks):
        udh = ports
        if len(chunks) > 1:
            udh = bytes([0x00, 0x03, reference & 0xFF, len(chunks), index + 1]) + ports
        ud = bytes([len(udh)]) + udh + chunk
        pdus.append(
            b"\x00"
            + b"\x44"
            + address
            + b"\x00"
            + b"\x04"
            + timestamp
            + bytes([len(ud)])
            + ud
        )
    return pdus

def png_1x1() -> bytes:
    import zlib
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    raw = b"\x00\xff\x00\x00"
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))
