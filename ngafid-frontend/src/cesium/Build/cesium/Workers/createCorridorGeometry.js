define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./arrayRemoveDuplicates-a580a060", "./EllipsoidTangentPlane-30395e74", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolylineVolumeGeometryLibrary-6dcd44cb", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2", "./CorridorGeometryLibrary-65ac78bb"], function (at, t, it, ot, h, e, r, nt, st, lt, dt, a, i, x, N, g, o, n, D, p, s, l, ut) {
    "use strict";
    var mt = new ot.Cartesian3, ft = new ot.Cartesian3, yt = new ot.Cartesian3, pt = new ot.Cartesian3,
        M = new ot.Cartesian3, ct = new ot.Cartesian3, ht = new ot.Cartesian3, gt = new ot.Cartesian3;

    function C(t, e) {
        for (var r = 0; r < t.length; r++) t[r] = e.scaleToGeodeticSurface(t[r], t[r]);
        return t
    }

    function Ct(t, e, r, a, i, o) {
        var n = t.normals, s = t.tangents, l = t.bitangents,
            d = ot.Cartesian3.normalize(ot.Cartesian3.cross(r, e, ht), ht);
        o.normal && ut.CorridorGeometryLibrary.addAttribute(n, e, a, i), o.tangent && ut.CorridorGeometryLibrary.addAttribute(s, d, a, i), o.bitangent && ut.CorridorGeometryLibrary.addAttribute(l, r, a, i)
    }

    function O(t, e, r) {
        var a, i, o, n = t.positions, s = t.corners, l = t.endPositions, d = t.lefts, u = t.normals,
            m = new lt.GeometryAttributes, f = 0, y = 0, p = 0;
        for (i = 0; i < n.length; i += 2) f += o = n[i].length - 3, p += 2 * o, y += n[i + 1].length - 3;
        for (f += 3, y += 3, i = 0; i < s.length; i++) {
            a = s[i];
            var c = s[i].leftPositions;
            at.defined(c) ? f += o = c.length : y += o = s[i].rightPositions.length, p += o
        }
        var h, g = at.defined(l);
        g && (f += h = l[0].length - 3, y += h, p += 6 * (h /= 3));
        var C, b, v, A, _, w, T = f + y, G = new Float64Array(T), E = {
            normals: e.normal ? new Float32Array(T) : void 0,
            tangents: e.tangent ? new Float32Array(T) : void 0,
            bitangents: e.bitangent ? new Float32Array(T) : void 0
        }, V = 0, F = T - 1, L = mt, P = ft, x = h / 2, N = dt.IndexDatatype.createTypedArray(T / 3, p), D = 0;
        if (g) {
            w = yt, _ = pt;
            var M = l[0];
            for (L = ot.Cartesian3.fromArray(u, 0, L), P = ot.Cartesian3.fromArray(d, 0, P), i = 0; i < x; i++) w = ot.Cartesian3.fromArray(M, 3 * (x - 1 - i), w), _ = ot.Cartesian3.fromArray(M, 3 * (x + i), _), ut.CorridorGeometryLibrary.addAttribute(G, _, V), ut.CorridorGeometryLibrary.addAttribute(G, w, void 0, F), Ct(E, L, P, V, F, e), A = (b = V / 3) + 1, v = (C = (F - 2) / 3) - 1, N[D++] = C, N[D++] = b, N[D++] = v, N[D++] = v, N[D++] = b, N[D++] = A, V += 3, F -= 3
        }
        var O, I, S = 0, R = 0, k = n[S++], H = n[S++];
        for (G.set(k, V), G.set(H, F - H.length + 1), P = ot.Cartesian3.fromArray(d, R, P), o = H.length - 3, i = 0; i < o; i += 3) O = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(k, i, ht), ht), I = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(H, o - i, gt), gt), Ct(E, L = ot.Cartesian3.normalize(ot.Cartesian3.add(O, I, L), L), P, V, F, e), A = (b = V / 3) + 1, v = (C = (F - 2) / 3) - 1, N[D++] = C, N[D++] = b, N[D++] = v, N[D++] = v, N[D++] = b, N[D++] = A, V += 3, F -= 3;
        for (O = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(k, o, ht), ht), I = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(H, o, gt), gt), L = ot.Cartesian3.normalize(ot.Cartesian3.add(O, I, L), L), R += 3, i = 0; i < s.length; i++) {
            var z, U, B, Y = (a = s[i]).leftPositions, W = a.rightPositions, q = ct, J = yt, j = pt;
            if (L = ot.Cartesian3.fromArray(u, R, L), at.defined(Y)) {
                for (Ct(E, L, P, void 0, F, e), F -= 3, U = A, B = v, z = 0; z < Y.length / 3; z++) q = ot.Cartesian3.fromArray(Y, 3 * z, q), N[D++] = U, N[D++] = B - z - 1, N[D++] = B - z, ut.CorridorGeometryLibrary.addAttribute(G, q, void 0, F), J = ot.Cartesian3.fromArray(G, 3 * (B - z - 1), J), j = ot.Cartesian3.fromArray(G, 3 * U, j), Ct(E, L, P = ot.Cartesian3.normalize(ot.Cartesian3.subtract(J, j, P), P), void 0, F, e), F -= 3;
                q = ot.Cartesian3.fromArray(G, 3 * U, q), J = ot.Cartesian3.subtract(ot.Cartesian3.fromArray(G, 3 * B, J), q, J), j = ot.Cartesian3.subtract(ot.Cartesian3.fromArray(G, 3 * (B - z), j), q, j), Ct(E, L, P = ot.Cartesian3.normalize(ot.Cartesian3.add(J, j, P), P), V, void 0, e), V += 3
            } else {
                for (Ct(E, L, P, V, void 0, e), V += 3, U = v, B = A, z = 0; z < W.length / 3; z++) q = ot.Cartesian3.fromArray(W, 3 * z, q), N[D++] = U, N[D++] = B + z, N[D++] = B + z + 1, ut.CorridorGeometryLibrary.addAttribute(G, q, V), J = ot.Cartesian3.fromArray(G, 3 * U, J), j = ot.Cartesian3.fromArray(G, 3 * (B + z), j), Ct(E, L, P = ot.Cartesian3.normalize(ot.Cartesian3.subtract(J, j, P), P), V, void 0, e), V += 3;
                q = ot.Cartesian3.fromArray(G, 3 * U, q), J = ot.Cartesian3.subtract(ot.Cartesian3.fromArray(G, 3 * (B + z), J), q, J), j = ot.Cartesian3.subtract(ot.Cartesian3.fromArray(G, 3 * B, j), q, j), Ct(E, L, P = ot.Cartesian3.normalize(ot.Cartesian3.negate(ot.Cartesian3.add(j, J, P), P), P), void 0, F, e), F -= 3
            }
            for (k = n[S++], H = n[S++], k.splice(0, 3), H.splice(H.length - 3, 3), G.set(k, V), G.set(H, F - H.length + 1), o = H.length - 3, R += 3, P = ot.Cartesian3.fromArray(d, R, P), z = 0; z < H.length; z += 3) O = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(k, z, ht), ht), I = r.geodeticSurfaceNormal(ot.Cartesian3.fromArray(H, o - z, gt), gt), Ct(E, L = ot.Cartesian3.normalize(ot.Cartesian3.add(O, I, L), L), P, V, F, e), b = (A = V / 3) - 1, C = (v = (F - 2) / 3) + 1, N[D++] = C, N[D++] = b, N[D++] = v, N[D++] = v, N[D++] = b, N[D++] = A, V += 3, F -= 3;
            V -= 3, F += 3
        }
        if (Ct(E, L = ot.Cartesian3.fromArray(u, u.length - 3, L), P, V, F, e), g) {
            V += 3, F -= 3, w = yt, _ = pt;
            var K = l[1];
            for (i = 0; i < x; i++) w = ot.Cartesian3.fromArray(K, 3 * (h - i - 1), w), _ = ot.Cartesian3.fromArray(K, 3 * i, _), ut.CorridorGeometryLibrary.addAttribute(G, w, void 0, F), ut.CorridorGeometryLibrary.addAttribute(G, _, V), Ct(E, L, P, V, F, e), b = (A = V / 3) - 1, C = (v = (F - 2) / 3) + 1, N[D++] = C, N[D++] = b, N[D++] = v, N[D++] = v, N[D++] = b, N[D++] = A, V += 3, F -= 3
        }
        if (m.position = new st.GeometryAttribute({
            componentDatatype: nt.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: G
        }), e.st) {
            var Q, X, Z = new Float32Array(T / 3 * 2), $ = 0;
            if (g) {
                f /= 3, y /= 3;
                var tt, et = Math.PI / (h + 1);
                X = 1 / (f - h + 1), Q = 1 / (y - h + 1);
                var rt = h / 2;
                for (i = 1 + rt; i < h + 1; i++) tt = it.CesiumMath.PI_OVER_TWO + et * i, Z[$++] = Q * (1 + Math.cos(tt)), Z[$++] = .5 * (1 + Math.sin(tt));
                for (i = 1; i < y - h + 1; i++) Z[$++] = i * Q, Z[$++] = 0;
                for (i = h; rt < i; i--) tt = it.CesiumMath.PI_OVER_TWO - i * et, Z[$++] = 1 - Q * (1 + Math.cos(tt)), Z[$++] = .5 * (1 + Math.sin(tt));
                for (i = rt; 0 < i; i--) tt = it.CesiumMath.PI_OVER_TWO - et * i, Z[$++] = 1 - X * (1 + Math.cos(tt)), Z[$++] = .5 * (1 + Math.sin(tt));
                for (i = f - h; 0 < i; i--) Z[$++] = i * X, Z[$++] = 1;
                for (i = 1; i < 1 + rt; i++) tt = it.CesiumMath.PI_OVER_TWO + et * i, Z[$++] = X * (1 + Math.cos(tt)), Z[$++] = .5 * (1 + Math.sin(tt))
            } else {
                for (X = 1 / ((f /= 3) - 1), Q = 1 / ((y /= 3) - 1), i = 0; i < y; i++) Z[$++] = i * Q, Z[$++] = 0;
                for (i = f; 0 < i; i--) Z[$++] = (i - 1) * X, Z[$++] = 1
            }
            m.st = new st.GeometryAttribute({
                componentDatatype: nt.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: Z
            })
        }
        return e.normal && (m.normal = new st.GeometryAttribute({
            componentDatatype: nt.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: E.normals
        })), e.tangent && (m.tangent = new st.GeometryAttribute({
            componentDatatype: nt.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: E.tangents
        })), e.bitangent && (m.bitangent = new st.GeometryAttribute({
            componentDatatype: nt.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: E.bitangents
        })), {attributes: m, indices: N}
    }

    function I(t, e, r) {
        r[e++] = t[0], r[e++] = t[1], r[e++] = t[2];
        for (var a = 3; a < t.length; a += 3) {
            var i = t[a], o = t[a + 1], n = t[a + 2];
            r[e++] = i, r[e++] = o, r[e++] = n, r[e++] = i, r[e++] = o, r[e++] = n
        }
        return r[e++] = t[0], r[e++] = t[1], r[e++] = t[2], r
    }

    function b(t, e) {
        var r = new N.VertexFormat({
                position: e.position,
                normal: e.normal || e.bitangent || t.shadowVolume,
                tangent: e.tangent,
                bitangent: e.normal || e.bitangent,
                st: e.st
            }), a = t.ellipsoid, i = O(ut.CorridorGeometryLibrary.computePositions(t), r, a), o = t.height,
            n = t.extrudedHeight, s = i.attributes, l = i.indices, d = s.position.values, u = d.length,
            m = new Float64Array(6 * u), f = new Float64Array(u);
        f.set(d);
        var y, p = new Float64Array(4 * u);
        p = I(d = D.PolygonPipeline.scaleToGeodeticHeight(d, o, a), 0, p), p = I(f = D.PolygonPipeline.scaleToGeodeticHeight(f, n, a), 2 * u, p), m.set(d), m.set(f, u), m.set(p, 2 * u), s.position.values = m, s = function (t, e) {
            if (!(e.normal || e.tangent || e.bitangent || e.st)) return t;
            var r, a, i = t.position.values;
            (e.normal || e.bitangent) && (r = t.normal.values, a = t.bitangent.values);
            var o, n = t.position.values.length / 18, s = 3 * n, l = 2 * n, d = 2 * s;
            if (e.normal || e.bitangent || e.tangent) {
                var u = e.normal ? new Float32Array(6 * s) : void 0, m = e.tangent ? new Float32Array(6 * s) : void 0,
                    f = e.bitangent ? new Float32Array(6 * s) : void 0, y = mt, p = ft, c = yt, h = pt, g = M, C = ct,
                    b = d;
                for (o = 0; o < s; o += 3) {
                    var v = b + d;
                    y = ot.Cartesian3.fromArray(i, o, y), p = ot.Cartesian3.fromArray(i, o + s, p), c = ot.Cartesian3.fromArray(i, (o + 3) % s, c), p = ot.Cartesian3.subtract(p, y, p), c = ot.Cartesian3.subtract(c, y, c), h = ot.Cartesian3.normalize(ot.Cartesian3.cross(p, c, h), h), e.normal && (ut.CorridorGeometryLibrary.addAttribute(u, h, v), ut.CorridorGeometryLibrary.addAttribute(u, h, v + 3), ut.CorridorGeometryLibrary.addAttribute(u, h, b), ut.CorridorGeometryLibrary.addAttribute(u, h, b + 3)), (e.tangent || e.bitangent) && (C = ot.Cartesian3.fromArray(r, o, C), e.bitangent && (ut.CorridorGeometryLibrary.addAttribute(f, C, v), ut.CorridorGeometryLibrary.addAttribute(f, C, v + 3), ut.CorridorGeometryLibrary.addAttribute(f, C, b), ut.CorridorGeometryLibrary.addAttribute(f, C, b + 3)), e.tangent && (g = ot.Cartesian3.normalize(ot.Cartesian3.cross(C, h, g), g), ut.CorridorGeometryLibrary.addAttribute(m, g, v), ut.CorridorGeometryLibrary.addAttribute(m, g, v + 3), ut.CorridorGeometryLibrary.addAttribute(m, g, b), ut.CorridorGeometryLibrary.addAttribute(m, g, b + 3))), b += 6
                }
                if (e.normal) {
                    for (u.set(r), o = 0; o < s; o += 3) u[o + s] = -r[o], u[o + s + 1] = -r[o + 1], u[o + s + 2] = -r[o + 2];
                    t.normal.values = u
                } else t.normal = void 0;
                if (e.bitangent ? (f.set(a), f.set(a, s), t.bitangent.values = f) : t.bitangent = void 0, e.tangent) {
                    var A = t.tangent.values;
                    m.set(A), m.set(A, s), t.tangent.values = m
                }
            }
            if (e.st) {
                var _ = t.st.values, w = new Float32Array(6 * l);
                w.set(_), w.set(_, l);
                for (var T = 2 * l, G = 0; G < 2; G++) {
                    for (w[T++] = _[0], w[T++] = _[1], o = 2; o < l; o += 2) {
                        var E = _[o], V = _[o + 1];
                        w[T++] = E, w[T++] = V, w[T++] = E, w[T++] = V
                    }
                    w[T++] = _[0], w[T++] = _[1]
                }
                t.st.values = w
            }
            return t
        }(s, e);
        var c = u / 3;
        if (t.shadowVolume) {
            var h = s.normal.values;
            u = h.length;
            var g = new Float32Array(6 * u);
            for (y = 0; y < u; y++) h[y] = -h[y];
            g.set(h, u), g = I(h, 4 * u, g), s.extrudeDirection = new st.GeometryAttribute({
                componentDatatype: nt.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: g
            }), e.normal || (s.normal = void 0)
        }
        if (at.defined(t.offsetAttribute)) {
            var C = new Uint8Array(6 * c);
            if (t.offsetAttribute === x.GeometryOffsetAttribute.TOP) C = x.arrayFill(C, 1, 0, c), C = x.arrayFill(C, 1, 2 * c, 4 * c); else {
                var b = t.offsetAttribute === x.GeometryOffsetAttribute.NONE ? 0 : 1;
                C = x.arrayFill(C, b)
            }
            s.applyOffset = new st.GeometryAttribute({
                componentDatatype: nt.ComponentDatatype.UNSIGNED_BYTE,
                componentsPerAttribute: 1,
                values: C
            })
        }
        var v = l.length, A = c + c, _ = dt.IndexDatatype.createTypedArray(m.length / 3, 2 * v + 3 * A);
        _.set(l);
        var w, T, G, E, V = v;
        for (y = 0; y < v; y += 3) {
            var F = l[y], L = l[y + 1], P = l[y + 2];
            _[V++] = P + c, _[V++] = L + c, _[V++] = F + c
        }
        for (y = 0; y < A; y += 2) G = (w = y + A) + 1, E = (T = w + A) + 1, _[V++] = w, _[V++] = T, _[V++] = G, _[V++] = G, _[V++] = T, _[V++] = E;
        return {attributes: s, indices: _}
    }

    var c = new ot.Cartesian3, v = new ot.Cartesian3, A = new ot.Cartographic;

    function _(t, e, r, a, i, o) {
        var n = ot.Cartesian3.subtract(e, t, c);
        ot.Cartesian3.normalize(n, n);
        var s = r.geodeticSurfaceNormal(t, v), l = ot.Cartesian3.cross(n, s, c);
        ot.Cartesian3.multiplyByScalar(l, a, l);
        var d = i.latitude, u = i.longitude, m = o.latitude, f = o.longitude;
        ot.Cartesian3.add(t, l, v), r.cartesianToCartographic(v, A);
        var y = A.latitude, p = A.longitude;
        d = Math.min(d, y), u = Math.min(u, p), m = Math.max(m, y), f = Math.max(f, p), ot.Cartesian3.subtract(t, l, v), r.cartesianToCartographic(v, A), y = A.latitude, p = A.longitude, d = Math.min(d, y), u = Math.min(u, p), m = Math.max(m, y), f = Math.max(f, p), i.latitude = d, i.longitude = u, o.latitude = m, o.longitude = f
    }

    var w = new ot.Cartesian3, T = new ot.Cartesian3, G = new ot.Cartographic, E = new ot.Cartographic;

    function d(t, e, r, a, i) {
        t = C(t, e);
        var o = g.arrayRemoveDuplicates(t, ot.Cartesian3.equalsEpsilon), n = o.length;
        if (n < 2 || r <= 0) return new ot.Rectangle;
        var s, l, d = .5 * r;
        if (G.latitude = Number.POSITIVE_INFINITY, G.longitude = Number.POSITIVE_INFINITY, E.latitude = Number.NEGATIVE_INFINITY, E.longitude = Number.NEGATIVE_INFINITY, a === p.CornerType.ROUNDED) {
            var u = o[0];
            ot.Cartesian3.subtract(u, o[1], w), ot.Cartesian3.normalize(w, w), ot.Cartesian3.multiplyByScalar(w, d, w), ot.Cartesian3.add(u, w, T), e.cartesianToCartographic(T, A), s = A.latitude, l = A.longitude, G.latitude = Math.min(G.latitude, s), G.longitude = Math.min(G.longitude, l), E.latitude = Math.max(E.latitude, s), E.longitude = Math.max(E.longitude, l)
        }
        for (var m = 0; m < n - 1; ++m) _(o[m], o[m + 1], e, d, G, E);
        var f = o[n - 1];
        ot.Cartesian3.subtract(f, o[n - 2], w), ot.Cartesian3.normalize(w, w), ot.Cartesian3.multiplyByScalar(w, d, w), ot.Cartesian3.add(f, w, T), _(f, T, e, d, G, E), a === p.CornerType.ROUNDED && (e.cartesianToCartographic(T, A), s = A.latitude, l = A.longitude, G.latitude = Math.min(G.latitude, s), G.longitude = Math.min(G.longitude, l), E.latitude = Math.max(E.latitude, s), E.longitude = Math.max(E.longitude, l));
        var y = at.defined(i) ? i : new ot.Rectangle;
        return y.north = E.latitude, y.south = G.latitude, y.east = E.longitude, y.west = G.longitude, y
    }

    function V(t) {
        var e = (t = at.defaultValue(t, at.defaultValue.EMPTY_OBJECT)).positions, r = t.width,
            a = at.defaultValue(t.height, 0), i = at.defaultValue(t.extrudedHeight, a);
        this._positions = e, this._ellipsoid = ot.Ellipsoid.clone(at.defaultValue(t.ellipsoid, ot.Ellipsoid.WGS84)), this._vertexFormat = N.VertexFormat.clone(at.defaultValue(t.vertexFormat, N.VertexFormat.DEFAULT)), this._width = r, this._height = Math.max(a, i), this._extrudedHeight = Math.min(a, i), this._cornerType = at.defaultValue(t.cornerType, p.CornerType.ROUNDED), this._granularity = at.defaultValue(t.granularity, it.CesiumMath.RADIANS_PER_DEGREE), this._shadowVolume = at.defaultValue(t.shadowVolume, !1), this._workerName = "createCorridorGeometry", this._offsetAttribute = t.offsetAttribute, this._rectangle = void 0, this.packedLength = 1 + e.length * ot.Cartesian3.packedLength + ot.Ellipsoid.packedLength + N.VertexFormat.packedLength + 7
    }

    V.pack = function (t, e, r) {
        r = at.defaultValue(r, 0);
        var a = t._positions, i = a.length;
        e[r++] = i;
        for (var o = 0; o < i; ++o, r += ot.Cartesian3.packedLength) ot.Cartesian3.pack(a[o], e, r);
        return ot.Ellipsoid.pack(t._ellipsoid, e, r), r += ot.Ellipsoid.packedLength, N.VertexFormat.pack(t._vertexFormat, e, r), r += N.VertexFormat.packedLength, e[r++] = t._width, e[r++] = t._height, e[r++] = t._extrudedHeight, e[r++] = t._cornerType, e[r++] = t._granularity, e[r++] = t._shadowVolume ? 1 : 0, e[r] = at.defaultValue(t._offsetAttribute, -1), e
    };
    var F = ot.Ellipsoid.clone(ot.Ellipsoid.UNIT_SPHERE), L = new N.VertexFormat, P = {
        positions: void 0,
        ellipsoid: F,
        vertexFormat: L,
        width: void 0,
        height: void 0,
        extrudedHeight: void 0,
        cornerType: void 0,
        granularity: void 0,
        shadowVolume: void 0,
        offsetAttribute: void 0
    };
    return V.unpack = function (t, e, r) {
        e = at.defaultValue(e, 0);
        for (var a = t[e++], i = new Array(a), o = 0; o < a; ++o, e += ot.Cartesian3.packedLength) i[o] = ot.Cartesian3.unpack(t, e);
        var n = ot.Ellipsoid.unpack(t, e, F);
        e += ot.Ellipsoid.packedLength;
        var s = N.VertexFormat.unpack(t, e, L);
        e += N.VertexFormat.packedLength;
        var l = t[e++], d = t[e++], u = t[e++], m = t[e++], f = t[e++], y = 1 === t[e++], p = t[e];
        return at.defined(r) ? (r._positions = i, r._ellipsoid = ot.Ellipsoid.clone(n, r._ellipsoid), r._vertexFormat = N.VertexFormat.clone(s, r._vertexFormat), r._width = l, r._height = d, r._extrudedHeight = u, r._cornerType = m, r._granularity = f, r._shadowVolume = y, r._offsetAttribute = -1 === p ? void 0 : p, r) : (P.positions = i, P.width = l, P.height = d, P.extrudedHeight = u, P.cornerType = m, P.granularity = f, P.shadowVolume = y, P.offsetAttribute = -1 === p ? void 0 : p, new V(P))
    }, V.computeRectangle = function (t, e) {
        var r = (t = at.defaultValue(t, at.defaultValue.EMPTY_OBJECT)).positions, a = t.width;
        return d(r, at.defaultValue(t.ellipsoid, ot.Ellipsoid.WGS84), a, at.defaultValue(t.cornerType, p.CornerType.ROUNDED), e)
    }, V.createGeometry = function (t) {
        var e = t._positions, r = t._width, a = t._ellipsoid;
        e = C(e, a);
        var i = g.arrayRemoveDuplicates(e, ot.Cartesian3.equalsEpsilon);
        if (!(i.length < 2 || r <= 0)) {
            var o, n = t._height, s = t._extrudedHeight,
                l = !it.CesiumMath.equalsEpsilon(n, s, 0, it.CesiumMath.EPSILON2), d = t._vertexFormat, u = {
                    ellipsoid: a,
                    positions: i,
                    width: r,
                    cornerType: t._cornerType,
                    granularity: t._granularity,
                    saveAttributes: !0
                };
            if (l) u.height = n, u.extrudedHeight = s, u.shadowVolume = t._shadowVolume, u.offsetAttribute = t._offsetAttribute, o = b(u, d); else if ((o = O(ut.CorridorGeometryLibrary.computePositions(u), d, a)).attributes.position.values = D.PolygonPipeline.scaleToGeodeticHeight(o.attributes.position.values, n, a), at.defined(t._offsetAttribute)) {
                var m = t._offsetAttribute === x.GeometryOffsetAttribute.NONE ? 0 : 1,
                    f = o.attributes.position.values.length, y = new Uint8Array(f / 3);
                x.arrayFill(y, m), o.attributes.applyOffset = new st.GeometryAttribute({
                    componentDatatype: nt.ComponentDatatype.UNSIGNED_BYTE,
                    componentsPerAttribute: 1,
                    values: y
                })
            }
            var p = o.attributes, c = h.BoundingSphere.fromVertices(p.position.values, void 0, 3);
            return d.position || (o.attributes.position.values = void 0), new st.Geometry({
                attributes: p,
                indices: o.indices,
                primitiveType: st.PrimitiveType.TRIANGLES,
                boundingSphere: c,
                offsetAttribute: t._offsetAttribute
            })
        }
    }, V.createShadowVolume = function (t, e, r) {
        var a = t._granularity, i = t._ellipsoid, o = e(a, i), n = r(a, i);
        return new V({
            positions: t._positions,
            width: t._width,
            cornerType: t._cornerType,
            ellipsoid: i,
            granularity: a,
            extrudedHeight: o,
            height: n,
            vertexFormat: N.VertexFormat.POSITION_ONLY,
            shadowVolume: !0
        })
    }, Object.defineProperties(V.prototype, {
        rectangle: {
            get: function () {
                return at.defined(this._rectangle) || (this._rectangle = d(this._positions, this._ellipsoid, this._width, this._cornerType)), this._rectangle
            }
        }, textureCoordinateRotationPoints: {
            get: function () {
                return [0, 0, 0, 1, 1, 0]
            }
        }
    }), function (t, e) {
        return at.defined(e) && (t = V.unpack(t, e)), t._ellipsoid = ot.Ellipsoid.clone(t._ellipsoid), V.createGeometry(t)
    }
});
