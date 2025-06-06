define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./GeometryAttribute-4098b8de"], function (t, f, n, b, O, a, G) {
    "use strict";
    var p = Math.cos, v = Math.sin, x = Math.sqrt, r = {
            computePosition: function (t, n, a, r, e, o, s) {
                var i = n.radiiSquared, g = t.nwCorner, h = t.boundingRectangle,
                    u = g.latitude - t.granYCos * r + e * t.granXSin, c = p(u), C = v(u), l = i.z * C,
                    d = g.longitude + r * t.granYSin + e * t.granXCos, S = c * p(d), w = c * v(d), M = i.x * S, X = i.y * w,
                    Y = x(M * S + X * w + l * C);
                if (o.x = M / Y, o.y = X / Y, o.z = l / Y, a) {
                    var m = t.stNwCorner;
                    f.defined(m) ? (u = m.latitude - t.stGranYCos * r + e * t.stGranXSin, d = m.longitude + r * t.stGranYSin + e * t.stGranXCos, s.x = (d - t.stWest) * t.lonScalar, s.y = (u - t.stSouth) * t.latScalar) : (s.x = (d - h.west) * t.lonScalar, s.y = (u - h.south) * t.latScalar)
                }
            }
        }, R = new G.Matrix2, y = new O.Cartesian3, P = new O.Cartographic, W = new O.Cartesian3,
        _ = new a.GeographicProjection;

    function T(t, n, a, r, e, o, s) {
        var i = Math.cos(n), g = r * i, h = a * i, u = Math.sin(n), c = r * u, C = a * u;
        y = _.project(t, y), y = O.Cartesian3.subtract(y, W, y);
        var l = G.Matrix2.fromRotation(n, R);
        y = G.Matrix2.multiplyByVector(l, y, y), y = O.Cartesian3.add(y, W, y), --o, --s;
        var d = (t = _.unproject(y, t)).latitude, S = d + o * C, w = d - g * s, M = d - g * s + o * C,
            X = Math.max(d, S, w, M), Y = Math.min(d, S, w, M), m = t.longitude, f = m + o * h, p = m + s * c,
            v = m + s * c + o * h;
        return {
            north: X,
            south: Y,
            east: Math.max(m, f, p, v),
            west: Math.min(m, f, p, v),
            granYCos: g,
            granYSin: c,
            granXCos: h,
            granXSin: C,
            nwCorner: t
        }
    }

    r.computeOptions = function (t, n, a, r, e, o, s) {
        var i, g, h, u, c, C = t.east, l = t.west, d = t.north, S = t.south, w = !1, M = !1;
        d === b.CesiumMath.PI_OVER_TWO && (w = !0), S === -b.CesiumMath.PI_OVER_TWO && (M = !0);
        var X = d - S;
        h = (c = C < l ? b.CesiumMath.TWO_PI - l + C : C - l) / ((i = Math.ceil(c / n) + 1) - 1), u = X / ((g = Math.ceil(X / n) + 1) - 1);
        var Y = O.Rectangle.northwest(t, o), m = O.Rectangle.center(t, P);
        0 === a && 0 === r || (m.longitude < Y.longitude && (m.longitude += b.CesiumMath.TWO_PI), W = _.project(m, W));
        var f = u, p = h, v = O.Rectangle.clone(t, e), G = {
            granYCos: f,
            granYSin: 0,
            granXCos: p,
            granXSin: 0,
            nwCorner: Y,
            boundingRectangle: v,
            width: i,
            height: g,
            northCap: w,
            southCap: M
        };
        if (0 !== a) {
            var x = T(Y, a, h, u, 0, i, g);
            d = x.north, S = x.south, C = x.east, l = x.west, G.granYCos = x.granYCos, G.granYSin = x.granYSin, G.granXCos = x.granXCos, G.granXSin = x.granXSin, v.north = d, v.south = S, v.east = C, v.west = l
        }
        if (0 !== r) {
            a -= r;
            var R = O.Rectangle.northwest(v, s), y = T(R, a, h, u, 0, i, g);
            G.stGranYCos = y.granYCos, G.stGranXCos = y.granXCos, G.stGranYSin = y.granYSin, G.stGranXSin = y.granXSin, G.stNwCorner = R, G.stWest = y.west, G.stSouth = y.south
        }
        return G
    }, t.RectangleGeometryLibrary = r
});
