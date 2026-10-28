#!/usr/bin/env python3
"""Draws the v1 -> v2 chart from PIT XML reports or typed-in scores.
Usage: python3 scripts/mutation_chart.py v1/mutations.xml v2/mutations.xml
   or: python3 scripts/mutation_chart.py --scores 52.4 81.0"""
import sys, xml.etree.ElementTree as ET
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
def score(path):
    ms = ET.parse(path).getroot().findall("mutation")
    killed = sum(m.get("status") == "KILLED" for m in ms)
    return round(100*killed/len(ms), 1), killed, len(ms)   # same definition as PIT
if sys.argv[1] == "--scores":
    v1, v2 = float(sys.argv[2]), float(sys.argv[3]); lab = ["", ""]
else:
    (v1, k1, n1), (v2, k2, n2) = score(sys.argv[1]), score(sys.argv[2])
    lab = [f"{k1}/{n1} killed", f"{k2}/{n2} killed"]
fig, ax = plt.subplots(figsize=(6, 4.2))
bars = ax.bar(["v1: AI-only suite\n(Week 7)", "v2: hybrid suite\n(Week 11)"], [v1, v2], color=["#c0504d", "#2e7d32"], width=.55)
for b, v, l in zip(bars, [v1, v2], lab):
    ax.text(b.get_x()+b.get_width()/2, v+1.5, f"{v}%\n{l}".strip(), ha="center", fontsize=10)
ax.set_ylim(0, 115); ax.set_ylabel("Mutation score (%)")
ax.set_title(f"PIT mutation score: v1 -> v2  (+{round(v2-v1,1)} pts)\ndouble-entry-bookkeeping-api", pad=14)
plt.tight_layout(); plt.savefig("mutation_v1_v2.png", dpi=200); print("saved mutation_v1_v2.png")
