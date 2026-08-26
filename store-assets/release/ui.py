"""Tap by what is actually on screen, not by a stopwatch."""
import os, re, subprocess, sys, time

ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")

def sh(*a, **kw):
    return subprocess.run([ADB] + list(a), capture_output=True, text=True, **kw).stdout

def dump():
    subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/ui.xml"],
                   capture_output=True, text=True)
    return sh("shell", "cat", "/sdcard/ui.xml")

def find(xml, needle, exact=False):
    """Centre of the first node whose text or content-desc matches, case-insensitively.

    exact matters for "Allow", which is also a substring of "Don't allow".
    """
    for m in re.finditer(r'<node[^>]*>', xml):
        n = m.group(0)
        t = (re.search(r'text="([^"]*)"', n) or [None, ""])[1]
        d = (re.search(r'content-desc="([^"]*)"', n) or [None, ""])[1]
        hit = ((t.lower(), d.lower()) if exact else (needle.lower() in t.lower(), needle.lower() in d.lower()))
        if (needle.lower() in hit if exact else (hit[0] or hit[1])):
            b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
            if b:
                x1, y1, x2, y2 = map(int, b.groups())
                return (x1 + x2) // 2, (y1 + y2) // 2
    return None

def wait_for(needle, timeout=25, exact=False):
    end = time.time() + timeout
    while time.time() < end:
        p = find(dump(), needle, exact)
        if p: return p
        time.sleep(0.7)
    raise SystemExit(f"never saw: {needle}")

def tap_text(needle, timeout=25, settle=0.6, exact=False):
    x, y = wait_for(needle, timeout, exact)
    time.sleep(settle)
    subprocess.run([ADB, "shell", "input", "tap", str(x), str(y)], capture_output=True)
    return x, y

def gone(needle, timeout=20):
    end = time.time() + timeout
    while time.time() < end:
        if not find(dump(), needle): return True
        time.sleep(0.7)
    return False

if __name__ == "__main__":
    print(find(dump(), sys.argv[1]))
