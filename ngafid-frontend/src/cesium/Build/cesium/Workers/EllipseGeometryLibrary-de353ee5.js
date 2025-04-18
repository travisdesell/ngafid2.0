define(["exports", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02"], function (a, R, W, c) {
    "use strict";
    var r = {}, x = new W.Cartesian3, M = new W.Cartesian3, d = new c.Quaternion, z = new c.Matrix3;

    function S(a, r, e, t, i, n, s, o, l, C) {
        var y = a + r;
        W.Cartesian3.multiplyByScalar(t, Math.cos(y), x), W.Cartesian3.multiplyByScalar(e, Math.sin(y), M), W.Cartesian3.add(x, M, x);
        var u = Math.cos(a);
        u *= u;
        var m = Math.sin(a);
        m *= m;
        var h = n / Math.sqrt(s * u + i * m) / o;
        return c.Quaternion.fromAxisAngle(x, h, d), c.Matrix3.fromQuaternion(d, z), c.Matrix3.multiplyByVector(z, l, C), W.Cartesian3.normalize(C, C), W.Cartesian3.multiplyByScalar(C, o, C), C
    }

    var B = new W.Cartesian3, b = new W.Cartesian3, Q = new W.Cartesian3, f = new W.Cartesian3;
    r.raisePositionsToHeight = function (a, r, e) {
        for (var t = r.ellipsoid, i = r.height, n = r.extrudedHeight, s = e ? a.length / 3 * 2 : a.length / 3, o = new Float64Array(3 * s), l = a.length, C = e ? l : 0, y = 0; y < l; y += 3) {
            var u = y + 1, m = y + 2, h = W.Cartesian3.fromArray(a, y, B);
            t.scaleToGeodeticSurface(h, h);
            var c = W.Cartesian3.clone(h, b), x = t.geodeticSurfaceNormal(h, f),
                M = W.Cartesian3.multiplyByScalar(x, i, Q);
            W.Cartesian3.add(h, M, h), e && (W.Cartesian3.multiplyByScalar(x, n, M), W.Cartesian3.add(c, M, c), o[y + C] = c.x, o[u + C] = c.y, o[m + C] = c.z), o[y] = h.x, o[u] = h.y, o[m] = h.z
        }
        return o
    };
    var G = new W.Cartesian3, H = new W.Cartesian3, N = new W.Cartesian3;
    r.computeEllipsePositions = function (a, r, e) {
        var t = a.semiMinorAxis, i = a.semiMajorAxis, n = a.rotation, s = a.center, o = 8 * a.granularity, l = t * t,
            C = i * i, y = i * t, u = W.Cartesian3.magnitude(s), m = W.Cartesian3.normalize(s, G),
            h = W.Cartesian3.cross(W.Cartesian3.UNIT_Z, s, H);
        h = W.Cartesian3.normalize(h, h);
        var c = W.Cartesian3.cross(m, h, N), x = 1 + Math.ceil(R.CesiumMath.PI_OVER_TWO / o),
            M = R.CesiumMath.PI_OVER_TWO / (x - 1), d = R.CesiumMath.PI_OVER_TWO - x * M;
        d < 0 && (x -= Math.ceil(Math.abs(d) / M));
        var z, f, _, v, O, p = r ? new Array(3 * (x * (x + 2) * 2)) : void 0, w = 0, P = B, T = b, I = 4 * x * 3,
            g = I - 1, E = 0, V = e ? new Array(I) : void 0;
        for (P = S(d = R.CesiumMath.PI_OVER_TWO, n, c, h, l, y, C, u, m, P), r && (p[w++] = P.x, p[w++] = P.y, p[w++] = P.z), e && (V[g--] = P.z, V[g--] = P.y, V[g--] = P.x), d = R.CesiumMath.PI_OVER_TWO - M, z = 1; z < x + 1; ++z) {
            if (P = S(d, n, c, h, l, y, C, u, m, P), T = S(Math.PI - d, n, c, h, l, y, C, u, m, T), r) {
                for (p[w++] = P.x, p[w++] = P.y, p[w++] = P.z, _ = 2 * z + 2, f = 1; f < _ - 1; ++f) v = f / (_ - 1), O = W.Cartesian3.lerp(P, T, v, Q), p[w++] = O.x, p[w++] = O.y, p[w++] = O.z;
                p[w++] = T.x, p[w++] = T.y, p[w++] = T.z
            }
            e && (V[g--] = P.z, V[g--] = P.y, V[g--] = P.x, V[E++] = T.x, V[E++] = T.y, V[E++] = T.z), d = R.CesiumMath.PI_OVER_TWO - (z + 1) * M
        }
        for (z = x; 1 < z; --z) {
            if (P = S(-(d = R.CesiumMath.PI_OVER_TWO - (z - 1) * M), n, c, h, l, y, C, u, m, P), T = S(d + Math.PI, n, c, h, l, y, C, u, m, T), r) {
                for (p[w++] = P.x, p[w++] = P.y, p[w++] = P.z, _ = 2 * (z - 1) + 2, f = 1; f < _ - 1; ++f) v = f / (_ - 1), O = W.Cartesian3.lerp(P, T, v, Q), p[w++] = O.x, p[w++] = O.y, p[w++] = O.z;
                p[w++] = T.x, p[w++] = T.y, p[w++] = T.z
            }
            e && (V[g--] = P.z, V[g--] = P.y, V[g--] = P.x, V[E++] = T.x, V[E++] = T.y, V[E++] = T.z)
        }
        P = S(-(d = R.CesiumMath.PI_OVER_TWO), n, c, h, l, y, C, u, m, P);
        var A = {};
        return r && (p[w++] = P.x, p[w++] = P.y, p[w++] = P.z, A.positions = p, A.numPts = x), e && (V[g--] = P.z, V[g--] = P.y, V[g--] = P.x, A.outerPositions = V), A
    }, a.EllipseGeometryLibrary = r
});
