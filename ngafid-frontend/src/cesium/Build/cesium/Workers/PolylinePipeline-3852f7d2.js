define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./EllipsoidRhumbLine-5134246a", "./EllipsoidGeodesic-19bdf744"], function (a, T, e, w, P, v, m, y, A, r) {
    "use strict";
    var E = {
        numberOfPoints: function (a, e, r) {
            var t = P.Cartesian3.distance(a, e);
            return Math.ceil(t / r)
        }, numberOfPointsRhumbLine: function (a, e, r) {
            var t = Math.pow(a.longitude - e.longitude, 2) + Math.pow(a.latitude - e.latitude, 2);
            return Math.ceil(Math.sqrt(t / (r * r)))
        }
    }, o = new P.Cartographic;
    E.extractHeights = function (a, e) {
        for (var r = a.length, t = new Array(r), i = 0; i < r; i++) {
            var n = a[i];
            t[i] = e.cartesianToCartographic(n, o).height
        }
        return t
    };
    var S = new v.Matrix4, b = new P.Cartesian3, R = new P.Cartesian3, M = new y.Plane(P.Cartesian3.UNIT_X, 0),
        D = new P.Cartesian3, G = new y.Plane(P.Cartesian3.UNIT_X, 0), x = new P.Cartesian3, N = new P.Cartesian3,
        I = [];

    function k(a, e, r) {
        var t, i = I;
        if (i.length = a, e === r) {
            for (t = 0; t < a; t++) i[t] = e;
            return i
        }
        var n = (r - e) / a;
        for (t = 0; t < a; t++) {
            var o = e + t * n;
            i[t] = o
        }
        return i
    }

    var V = new P.Cartographic, L = new P.Cartographic, _ = new P.Cartesian3, O = new P.Cartesian3,
        B = new P.Cartesian3, U = new r.EllipsoidGeodesic, z = new A.EllipsoidRhumbLine;

    function X(a, e, r, t, i, n, o, s) {
        var c = t.scaleToGeodeticSurface(a, O), l = t.scaleToGeodeticSurface(e, B), u = E.numberOfPoints(a, e, r),
            h = t.cartesianToCartographic(c, V), f = t.cartesianToCartographic(l, L), d = k(u, i, n);
        U.setEndPoints(h, f);
        var C = U.surfaceDistance / u, g = s;
        h.height = i;
        var p = t.cartographicToCartesian(h, _);
        P.Cartesian3.pack(p, o, g), g += 3;
        for (var v = 1; v < u; v++) {
            var m = U.interpolateUsingSurfaceDistance(v * C, L);
            m.height = d[v], p = t.cartographicToCartesian(m, _), P.Cartesian3.pack(p, o, g), g += 3
        }
        return g
    }

    function q(a, e, r, t, i, n, o, s) {
        var c = t.scaleToGeodeticSurface(a, O), l = t.scaleToGeodeticSurface(e, B), u = t.cartesianToCartographic(c, V),
            h = t.cartesianToCartographic(l, L), f = E.numberOfPointsRhumbLine(u, h, r), d = k(f, i, n);
        z.ellipsoid.equals(t) || (z = new A.EllipsoidRhumbLine(void 0, void 0, t)), z.setEndPoints(u, h);
        var C = z.surfaceDistance / f, g = s;
        u.height = i;
        var p = t.cartographicToCartesian(u, _);
        P.Cartesian3.pack(p, o, g), g += 3;
        for (var v = 1; v < f; v++) {
            var m = z.interpolateUsingSurfaceDistance(v * C, L);
            m.height = d[v], p = t.cartographicToCartesian(m, _), P.Cartesian3.pack(p, o, g), g += 3
        }
        return g
    }

    E.wrapLongitude = function (a, e) {
        var r = [], t = [];
        if (T.defined(a) && 0 < a.length) {
            e = T.defaultValue(e, v.Matrix4.IDENTITY);
            var i = v.Matrix4.inverseTransformation(e, S), n = v.Matrix4.multiplyByPoint(i, P.Cartesian3.ZERO, b),
                o = P.Cartesian3.normalize(v.Matrix4.multiplyByPointAsVector(i, P.Cartesian3.UNIT_Y, R), R),
                s = y.Plane.fromPointNormal(n, o, M),
                c = P.Cartesian3.normalize(v.Matrix4.multiplyByPointAsVector(i, P.Cartesian3.UNIT_X, D), D),
                l = y.Plane.fromPointNormal(n, c, G), u = 1;
            r.push(P.Cartesian3.clone(a[0]));
            for (var h = r[0], f = a.length, d = 1; d < f; ++d) {
                var C = a[d];
                if (y.Plane.getPointDistance(l, h) < 0 || y.Plane.getPointDistance(l, C) < 0) {
                    var g = m.IntersectionTests.lineSegmentPlane(h, C, s, x);
                    if (T.defined(g)) {
                        var p = P.Cartesian3.multiplyByScalar(o, 5e-9, N);
                        y.Plane.getPointDistance(s, h) < 0 && P.Cartesian3.negate(p, p), r.push(P.Cartesian3.add(g, p, new P.Cartesian3)), t.push(u + 1), P.Cartesian3.negate(p, p), r.push(P.Cartesian3.add(g, p, new P.Cartesian3)), u = 1
                    }
                }
                r.push(P.Cartesian3.clone(a[d])), u++, h = C
            }
            t.push(u)
        }
        return {positions: r, lengths: t}
    }, E.generateArc = function (a) {
        T.defined(a) || (a = {});
        var e = a.positions, r = e.length, t = T.defaultValue(a.ellipsoid, P.Ellipsoid.WGS84),
            i = T.defaultValue(a.height, 0), n = Array.isArray(i);
        if (r < 1) return [];
        if (1 === r) {
            var o = t.scaleToGeodeticSurface(e[0], O);
            if (0 !== (i = n ? i[0] : i)) {
                var s = t.geodeticSurfaceNormal(o, _);
                P.Cartesian3.multiplyByScalar(s, i, s), P.Cartesian3.add(o, s, o)
            }
            return [o.x, o.y, o.z]
        }
        var c = a.minDistance;
        if (!T.defined(c)) {
            var l = T.defaultValue(a.granularity, w.CesiumMath.RADIANS_PER_DEGREE);
            c = w.CesiumMath.chordLength(l, t.maximumRadius)
        }
        var u, h = 0;
        for (u = 0; u < r - 1; u++) h += E.numberOfPoints(e[u], e[u + 1], c);
        var f = 3 * (h + 1), d = new Array(f), C = 0;
        for (u = 0; u < r - 1; u++) {
            C = X(e[u], e[u + 1], c, t, n ? i[u] : i, n ? i[u + 1] : i, d, C)
        }
        I.length = 0;
        var g = e[r - 1], p = t.cartesianToCartographic(g, V);
        p.height = n ? i[r - 1] : i;
        var v = t.cartographicToCartesian(p, _);
        return P.Cartesian3.pack(v, d, f - 3), d
    };
    var W = new P.Cartographic, Y = new P.Cartographic;
    E.generateRhumbArc = function (a) {
        T.defined(a) || (a = {});
        var e = a.positions, r = e.length, t = T.defaultValue(a.ellipsoid, P.Ellipsoid.WGS84),
            i = T.defaultValue(a.height, 0), n = Array.isArray(i);
        if (r < 1) return [];
        if (1 === r) {
            var o = t.scaleToGeodeticSurface(e[0], O);
            if (0 !== (i = n ? i[0] : i)) {
                var s = t.geodeticSurfaceNormal(o, _);
                P.Cartesian3.multiplyByScalar(s, i, s), P.Cartesian3.add(o, s, o)
            }
            return [o.x, o.y, o.z]
        }
        var c, l, u = T.defaultValue(a.granularity, w.CesiumMath.RADIANS_PER_DEGREE), h = 0,
            f = t.cartesianToCartographic(e[0], W);
        for (c = 0; c < r - 1; c++) l = t.cartesianToCartographic(e[c + 1], Y), h += E.numberOfPointsRhumbLine(f, l, u), f = P.Cartographic.clone(l, W);
        var d = 3 * (h + 1), C = new Array(d), g = 0;
        for (c = 0; c < r - 1; c++) {
            g = q(e[c], e[c + 1], u, t, n ? i[c] : i, n ? i[c + 1] : i, C, g)
        }
        I.length = 0;
        var p = e[r - 1], v = t.cartesianToCartographic(p, V);
        v.height = n ? i[r - 1] : i;
        var m = t.cartographicToCartesian(v, _);
        return P.Cartesian3.pack(m, C, d - 3), C
    }, E.generateCartesianArc = function (a) {
        for (var e = E.generateArc(a), r = e.length / 3, t = new Array(r), i = 0; i < r; i++) t[i] = P.Cartesian3.unpack(e, 3 * i);
        return t
    }, E.generateCartesianRhumbArc = function (a) {
        for (var e = E.generateRhumbArc(a), r = e.length / 3, t = new Array(r), i = 0; i < r; i++) t[i] = P.Cartesian3.unpack(e, 3 * i);
        return t
    }, a.PolylinePipeline = E
});
