#!/usr/bin/env python3
"""WCAG text contrast checks for actual semantic color pairs (light/dark)."""
def luminance(color):
    values=[int(color[i:i+2],16)/255 for i in (0,2,4)]
    linear=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in values]
    return sum(v*w for v,w in zip(linear,(.2126,.7152,.0722)))
pairs=[('light body','101B35','F8FAFF'),('light secondary','53617A','F0F5FE'),('light button','FFFFFF','1764DF'),('light banner','103E82','E5EFFF'),('dark body','E6EDFA','172033'),('dark secondary','BAC7DD','1C2A42'),('dark button','003062','A8C7FF')]
for label,a,b in pairs:
    x,y=sorted((luminance(a),luminance(b)));ratio=(y+.05)/(x+.05)
    assert ratio>=4.5,(label,ratio)
    print(label,round(ratio,2))
