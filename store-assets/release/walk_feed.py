"""Feed the emulator a walk along the real WBW trail: one fix a second at a
brisk walking pace, interpolated between the route's own points."""
import json, math, os, subprocess, sys, time

S = os.path.dirname(os.path.abspath(__file__))
ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")
pts = json.load(open(S + "/route.json"))

SPEED = float(sys.argv[1]) if len(sys.argv) > 1 else 2.5      # metres per second
DURATION = float(sys.argv[2]) if len(sys.argv) > 2 else 240   # seconds
START_M = float(sys.argv[3]) if len(sys.argv) > 3 else 0      # metres along route

def metres(a, b):
    R = 6371000.0
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp = p2 - p1
    dl = math.radians(b[1] - a[1])
    h = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2*R*math.asin(math.sqrt(h))

# cumulative distance along the route
cum = [0.0]
for i in range(1, len(pts)):
    cum.append(cum[-1] + metres(pts[i-1], pts[i]))

def at(dist):
    if dist <= 0: return pts[0]
    if dist >= cum[-1]: return pts[-1]
    lo, hi = 0, len(cum)-1
    while lo < hi-1:
        mid = (lo+hi)//2
        if cum[mid] <= dist: lo = mid
        else: hi = mid
    span = cum[hi]-cum[lo]
    t = 0 if span == 0 else (dist-cum[lo])/span
    return (pts[lo][0] + (pts[hi][0]-pts[lo][0])*t,
            pts[lo][1] + (pts[hi][1]-pts[lo][1])*t)

t0 = time.monotonic()
while True:
    elapsed = time.monotonic() - t0
    if elapsed > DURATION: break
    lat, lng = at(START_M + elapsed*SPEED)
    subprocess.run([ADB, "emu", "geo", "fix", f"{lng:.6f}", f"{lat:.6f}"],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1)
