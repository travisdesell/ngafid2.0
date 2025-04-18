define(["exports", "./Math-d30358ed"], function (r, b) {
    "use strict";
    var t = {
        computePositions: function (r, t, e, a, i) {
            var n, o = .5 * r, s = -o, u = a + a, c = new Float64Array(3 * (i ? 2 * u : u)), d = 0, f = 0,
                h = i ? 3 * u : 0, y = i ? 3 * (u + a) : 3 * a;
            for (n = 0; n < a; n++) {
                var M = n / a * b.CesiumMath.TWO_PI, m = Math.cos(M), v = Math.sin(M), l = m * e, p = v * e, C = m * t,
                    P = v * t;
                c[f + h] = l, c[f + h + 1] = p, c[f + h + 2] = s, c[f + y] = C, c[f + y + 1] = P, c[f + y + 2] = o, f += 3, i && (c[d++] = l, c[d++] = p, c[d++] = s, c[d++] = C, c[d++] = P, c[d++] = o)
            }
            return c
        }
    };
    r.CylinderGeometryLibrary = t
});
