define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2", "./Color-9e7980a7"], function (S, e, I, R, O, o, r, M, U, N, F, t, a, H, l, i, W, Y) {
    "use strict";

    function q(e, o, r, t, a, l, i) {
        var n, s = W.PolylinePipeline.numberOfPoints(e, o, a), p = r.red, d = r.green, f = r.blue, y = r.alpha,
            c = t.red, u = t.green, h = t.blue, C = t.alpha;
        if (Y.Color.equals(r, t)) {
            for (n = 0; n < s; n++) l[i++] = Y.Color.floatToByte(p), l[i++] = Y.Color.floatToByte(d), l[i++] = Y.Color.floatToByte(f), l[i++] = Y.Color.floatToByte(y);
            return i
        }
        var T = (c - p) / s, g = (u - d) / s, m = (h - f) / s, v = (C - y) / s, P = i;
        for (n = 0; n < s; n++) l[P++] = Y.Color.floatToByte(p + n * T), l[P++] = Y.Color.floatToByte(d + n * g), l[P++] = Y.Color.floatToByte(f + n * m), l[P++] = Y.Color.floatToByte(y + n * v);
        return P
    }

    function f(e) {
        var o = (e = S.defaultValue(e, S.defaultValue.EMPTY_OBJECT)).positions, r = e.colors,
            t = S.defaultValue(e.colorsPerVertex, !1);
        this._positions = o, this._colors = r, this._colorsPerVertex = t, this._arcType = S.defaultValue(e.arcType, H.ArcType.GEODESIC), this._granularity = S.defaultValue(e.granularity, I.CesiumMath.RADIANS_PER_DEGREE), this._ellipsoid = S.defaultValue(e.ellipsoid, R.Ellipsoid.WGS84), this._workerName = "createSimplePolylineGeometry";
        var a = 1 + o.length * R.Cartesian3.packedLength;
        a += S.defined(r) ? 1 + r.length * Y.Color.packedLength : 1, this.packedLength = a + R.Ellipsoid.packedLength + 3
    }

    f.pack = function (e, o, r) {
        var t;
        r = S.defaultValue(r, 0);
        var a = e._positions, l = a.length;
        for (o[r++] = l, t = 0; t < l; ++t, r += R.Cartesian3.packedLength) R.Cartesian3.pack(a[t], o, r);
        var i = e._colors;
        for (l = S.defined(i) ? i.length : 0, o[r++] = l, t = 0; t < l; ++t, r += Y.Color.packedLength) Y.Color.pack(i[t], o, r);
        return R.Ellipsoid.pack(e._ellipsoid, o, r), r += R.Ellipsoid.packedLength, o[r++] = e._colorsPerVertex ? 1 : 0, o[r++] = e._arcType, o[r] = e._granularity, o
    }, f.unpack = function (e, o, r) {
        var t;
        o = S.defaultValue(o, 0);
        var a = e[o++], l = new Array(a);
        for (t = 0; t < a; ++t, o += R.Cartesian3.packedLength) l[t] = R.Cartesian3.unpack(e, o);
        var i = 0 < (a = e[o++]) ? new Array(a) : void 0;
        for (t = 0; t < a; ++t, o += Y.Color.packedLength) i[t] = Y.Color.unpack(e, o);
        var n = R.Ellipsoid.unpack(e, o);
        o += R.Ellipsoid.packedLength;
        var s = 1 === e[o++], p = e[o++], d = e[o];
        return S.defined(r) ? (r._positions = l, r._colors = i, r._ellipsoid = n, r._colorsPerVertex = s, r._arcType = p, r._granularity = d, r) : new f({
            positions: l,
            colors: i,
            ellipsoid: n,
            colorsPerVertex: s,
            arcType: p,
            granularity: d
        })
    };
    var z = new Array(2), J = new Array(2),
        j = {positions: z, height: J, ellipsoid: void 0, minDistance: void 0, granularity: void 0};
    return f.createGeometry = function (e) {
        var o, r, t, a, l, i = e._positions, n = e._colors, s = e._colorsPerVertex, p = e._arcType, d = e._granularity,
            f = e._ellipsoid, y = I.CesiumMath.chordLength(d, f.maximumRadius), c = S.defined(n) && !s, u = i.length,
            h = 0;
        if (p === H.ArcType.GEODESIC || p === H.ArcType.RHUMB) {
            var C, T, g;
            g = p === H.ArcType.GEODESIC ? (C = I.CesiumMath.chordLength(d, f.maximumRadius), T = W.PolylinePipeline.numberOfPoints, W.PolylinePipeline.generateArc) : (C = d, T = W.PolylinePipeline.numberOfPointsRhumbLine, W.PolylinePipeline.generateRhumbArc);
            var m = W.PolylinePipeline.extractHeights(i, f), v = j;
            if (p === H.ArcType.GEODESIC ? v.minDistance = y : v.granularity = d, v.ellipsoid = f, c) {
                var P = 0;
                for (o = 0; o < u - 1; o++) P += T(i[o], i[o + 1], C) + 1;
                r = new Float64Array(3 * P), a = new Uint8Array(4 * P), v.positions = z, v.height = J;
                var _ = 0;
                for (o = 0; o < u - 1; ++o) {
                    z[0] = i[o], z[1] = i[o + 1], J[0] = m[o], J[1] = m[o + 1];
                    var B = g(v);
                    if (S.defined(n)) {
                        var A = B.length / 3;
                        l = n[o];
                        for (var E = 0; E < A; ++E) a[_++] = Y.Color.floatToByte(l.red), a[_++] = Y.Color.floatToByte(l.green), a[_++] = Y.Color.floatToByte(l.blue), a[_++] = Y.Color.floatToByte(l.alpha)
                    }
                    r.set(B, h), h += B.length
                }
            } else if (v.positions = i, v.height = m, r = new Float64Array(g(v)), S.defined(n)) {
                for (a = new Uint8Array(r.length / 3 * 4), o = 0; o < u - 1; ++o) {
                    h = q(i[o], i[o + 1], n[o], n[o + 1], y, a, h)
                }
                var b = n[u - 1];
                a[h++] = Y.Color.floatToByte(b.red), a[h++] = Y.Color.floatToByte(b.green), a[h++] = Y.Color.floatToByte(b.blue), a[h++] = Y.Color.floatToByte(b.alpha)
            }
        } else {
            t = c ? 2 * u - 2 : u, r = new Float64Array(3 * t), a = S.defined(n) ? new Uint8Array(4 * t) : void 0;
            var k = 0, G = 0;
            for (o = 0; o < u; ++o) {
                var w = i[o];
                if (c && 0 < o && (R.Cartesian3.pack(w, r, k), k += 3, l = n[o - 1], a[G++] = Y.Color.floatToByte(l.red), a[G++] = Y.Color.floatToByte(l.green), a[G++] = Y.Color.floatToByte(l.blue), a[G++] = Y.Color.floatToByte(l.alpha)), c && o === u - 1) break;
                R.Cartesian3.pack(w, r, k), k += 3, S.defined(n) && (l = n[o], a[G++] = Y.Color.floatToByte(l.red), a[G++] = Y.Color.floatToByte(l.green), a[G++] = Y.Color.floatToByte(l.blue), a[G++] = Y.Color.floatToByte(l.alpha))
            }
        }
        var D = new N.GeometryAttributes;
        D.position = new U.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: r
        }), S.defined(n) && (D.color = new U.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.UNSIGNED_BYTE,
            componentsPerAttribute: 4,
            values: a,
            normalize: !0
        }));
        var L = 2 * ((t = r.length / 3) - 1), V = F.IndexDatatype.createTypedArray(t, L), x = 0;
        for (o = 0; o < t - 1; ++o) V[x++] = o, V[x++] = o + 1;
        return new U.Geometry({
            attributes: D,
            indices: V,
            primitiveType: U.PrimitiveType.LINES,
            boundingSphere: O.BoundingSphere.fromPoints(i)
        })
    }, function (e, o) {
        return S.defined(o) && (e = f.unpack(e, o)), e._ellipsoid = R.Ellipsoid.clone(e._ellipsoid), f.createGeometry(e)
    }
});
