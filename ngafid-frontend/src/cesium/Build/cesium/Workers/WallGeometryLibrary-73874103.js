define(["exports", "./when-c2e8ef35", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./EllipsoidTangentPlane-30395e74", "./PolygonPipeline-84f0d07f", "./PolylinePipeline-3852f7d2"], function (e, C, A, w, E, O, M) {
    "use strict";
    var i = {};
    var b = new w.Cartographic, L = new w.Cartographic;
    var F = new Array(2), H = new Array(2),
        T = {positions: void 0, height: void 0, granularity: void 0, ellipsoid: void 0};
    i.computePositions = function (e, i, t, n, r, o) {
        var a = function (e, i, t, n) {
            var r = i.length;
            if (!(r < 2)) {
                var o = C.defined(n), a = C.defined(t), l = !0, h = new Array(r), s = new Array(r), g = new Array(r),
                    d = i[0];
                h[0] = d;
                var p = e.cartesianToCartographic(d, b);
                a && (p.height = t[0]), l = l && p.height <= 0, s[0] = p.height, g[0] = o ? n[0] : 0;
                for (var P, u, c = 1, v = 1; v < r; ++v) {
                    var f = i[v], y = e.cartesianToCartographic(f, L);
                    a && (y.height = t[v]), l = l && y.height <= 0, P = p, u = y, A.CesiumMath.equalsEpsilon(P.latitude, u.latitude, A.CesiumMath.EPSILON14) && A.CesiumMath.equalsEpsilon(P.longitude, u.longitude, A.CesiumMath.EPSILON14) ? p.height < y.height && (s[c - 1] = y.height) : (h[c] = f, s[c] = y.height, g[c] = o ? n[v] : 0, w.Cartographic.clone(y, p), ++c)
                }
                if (!(l || c < 2)) return h.length = c, s.length = c, g.length = c, {
                    positions: h,
                    topHeights: s,
                    bottomHeights: g
                }
            }
        }(e, i, t, n);
        if (C.defined(a)) {
            if (i = a.positions, t = a.topHeights, n = a.bottomHeights, 3 <= i.length) {
                var l = E.EllipsoidTangentPlane.fromPoints(i, e).projectPointsOntoPlane(i);
                O.PolygonPipeline.computeWindingOrder2D(l) === O.WindingOrder.CLOCKWISE && (i.reverse(), t.reverse(), n.reverse())
            }
            var h, s, g = i.length, d = g - 2, p = A.CesiumMath.chordLength(r, e.maximumRadius), P = T;
            if (P.minDistance = p, P.ellipsoid = e, o) {
                var u, c = 0;
                for (u = 0; u < g - 1; u++) c += M.PolylinePipeline.numberOfPoints(i[u], i[u + 1], p) + 1;
                h = new Float64Array(3 * c), s = new Float64Array(3 * c);
                var v = F, f = H;
                P.positions = v, P.height = f;
                var y = 0;
                for (u = 0; u < g - 1; u++) {
                    v[0] = i[u], v[1] = i[u + 1], f[0] = t[u], f[1] = t[u + 1];
                    var m = M.PolylinePipeline.generateArc(P);
                    h.set(m, y), f[0] = n[u], f[1] = n[u + 1], s.set(M.PolylinePipeline.generateArc(P), y), y += m.length
                }
            } else P.positions = i, P.height = t, h = new Float64Array(M.PolylinePipeline.generateArc(P)), P.height = n, s = new Float64Array(M.PolylinePipeline.generateArc(P));
            return {bottomPositions: s, topPositions: h, numCorners: d}
        }
    }, e.WallGeometryLibrary = i
});
