"""The FOREGROUND_SERVICE_LOCATION demo take, driven by what is on screen."""
import os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ui import ADB, tap_text, wait_for, dump, find

S = os.path.dirname(os.path.abspath(__file__))
def adb(*a): return subprocess.run([ADB]+list(a), capture_output=True, text=True)
def key(k): adb("shell", "input", "keyevent", k)
def shade(): adb("shell", "cmd", "statusbar", "expand-notifications")

# cold, logged in, no permissions granted, standing at the trail head
adb("shell", "am", "force-stop", "th.ac.mfu.su.wbw")
for p in ("ACCESS_FINE_LOCATION","ACCESS_COARSE_LOCATION","ACTIVITY_RECOGNITION","POST_NOTIFICATIONS"):
    adb("shell", "pm", "revoke", "th.ac.mfu.su.wbw", "android.permission."+p)
adb("emu", "geo", "fix", "99.89696", "20.04134")
adb("shell", "monkey", "-p", "th.ac.mfu.su.wbw", "-c", "android.intent.category.LAUNCHER", "1")
wait_for("Hi,", 40)
time.sleep(2)

rec = subprocess.Popen([ADB, "shell", "screenrecord", "--time-limit", "178",
                        "--bit-rate", "8000000", "--size", "720x1600", "/sdcard/wbw_fgs3.mp4"])
t0 = time.time()
def mark(what): print(f"{time.time()-t0:6.1f}s  {what}", flush=True)

time.sleep(4);                       mark("home screen")
tap_text("Map");                     mark("opened the trail map")
tap_text("While using the app", 30); mark("granted location")
wait_for("Start walking", 30); time.sleep(4)
tap_text("Start walking");           mark("tapped Start walking")
tap_text("Allow", 20, exact=True);   mark("allowed physical activity")
try:
    tap_text("Allow", 6, exact=True); mark("allowed notifications")
except SystemExit:
    mark("no notification prompt")
wait_for("Stop", 25);                mark("walk is running")

feed = subprocess.Popen(["python3", S+"/walk_feed.py", "2.5", "100", "0"])
time.sleep(30);                      mark("walked in the foreground")

shade(); time.sleep(10); key("KEYCODE_BACK"); time.sleep(2)
mark("showed the ongoing notification")

key("KEYCODE_HOME"); time.sleep(5)
shade(); time.sleep(22); key("KEYCODE_BACK"); time.sleep(2)
mark("showed it still tracking with the app in the background")

adb("shell", "monkey", "-p", "th.ac.mfu.su.wbw", "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(18);                      mark("back in the app, distance accrued")

feed.wait()
time.sleep(3)
tap_text("Stop"); time.sleep(6);     mark("stopped the walk")
shade(); time.sleep(6); key("KEYCODE_BACK"); time.sleep(2)
mark("notification gone")

rec.wait()
time.sleep(2)
adb("pull", "/sdcard/wbw_fgs3.mp4", S+"/wbw_fgs3.mp4")
mark("pulled")
