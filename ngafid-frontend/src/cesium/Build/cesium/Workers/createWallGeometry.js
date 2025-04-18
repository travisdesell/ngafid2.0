define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./EllipsoidTangentPlane-30395e74", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2", "./WallGeometryLibrary-73874103"], function (Z, e, j, K, Q, t, a, X, $, ee, te, i, n, p, r, o, s, m, l, ae) {
    "use strict";
    var ie = new K.Cartesian3, ne = new K.Cartesian3, re = new K.Cartesian3, oe = new K.Cartesian3,
        se = new K.Cartesian3, me = new K.Cartesian3, le = new K.Cartesian3, de = new K.Cartesian3;

    function u(e) {
        var t = (e = Z.defaultValue(e, Z.defaultValue.EMPTY_OBJECT)).positions, a = e.maximumHeights,
            i = e.minimumHeights, n = Z.defaultValue(e.vertexFormat, p.VertexFormat.DEFAULT),
            r = Z.defaultValue(e.granularity, j.CesiumMath.RADIANS_PER_DEGREE),
            o = Z.defaultValue(e.ellipsoid, K.Ellipsoid.WGS84);
        this._positions = t, this._minimumHeights = i, this._maximumHeights = a, this._vertexFormat = p.VertexFormat.clone(n), this._granularity = r, this._ellipsoid = K.Ellipsoid.clone(o), this._workerName = "createWallGeometry";
        var s = 1 + t.length * K.Cartesian3.packedLength + 2;
        Z.defined(i) && (s += i.length), Z.defined(a) && (s += a.length), this.packedLength = s + K.Ellipsoid.packedLength + p.VertexFormat.packedLength + 1
    }

    u.pack = function (e, t, a) {
        var i;
        a = Z.defaultValue(a, 0);
        var n = e._positions, r = n.length;
        for (t[a++] = r, i = 0; i < r; ++i, a += K.Cartesian3.packedLength) K.Cartesian3.pack(n[i], t, a);
        var o = e._minimumHeights;
        if (r = Z.defined(o) ? o.length : 0, t[a++] = r, Z.defined(o)) for (i = 0; i < r; ++i) t[a++] = o[i];
        var s = e._maximumHeights;
        if (r = Z.defined(s) ? s.length : 0, t[a++] = r, Z.defined(s)) for (i = 0; i < r; ++i) t[a++] = s[i];
        return K.Ellipsoid.pack(e._ellipsoid, t, a), a += K.Ellipsoid.packedLength, p.VertexFormat.pack(e._vertexFormat, t, a), t[a += p.VertexFormat.packedLength] = e._granularity, t
    };
    var f = K.Ellipsoid.clone(K.Ellipsoid.UNIT_SPHERE), c = new p.VertexFormat, y = {
        positions: void 0,
        minimumHeights: void 0,
        maximumHeights: void 0,
        ellipsoid: f,
        vertexFormat: c,
        granularity: void 0
    };
    return u.unpack = function (e, t, a) {
        var i;
        t = Z.defaultValue(t, 0);
        var n, r, o = e[t++], s = new Array(o);
        for (i = 0; i < o; ++i, t += K.Cartesian3.packedLength) s[i] = K.Cartesian3.unpack(e, t);
        if (0 < (o = e[t++])) for (n = new Array(o), i = 0; i < o; ++i) n[i] = e[t++];
        if (0 < (o = e[t++])) for (r = new Array(o), i = 0; i < o; ++i) r[i] = e[t++];
        var m = K.Ellipsoid.unpack(e, t, f);
        t += K.Ellipsoid.packedLength;
        var l = p.VertexFormat.unpack(e, t, c), d = e[t += p.VertexFormat.packedLength];
        return Z.defined(a) ? (a._positions = s, a._minimumHeights = n, a._maximumHeights = r, a._ellipsoid = K.Ellipsoid.clone(m, a._ellipsoid), a._vertexFormat = p.VertexFormat.clone(l, a._vertexFormat), a._granularity = d, a) : (y.positions = s, y.minimumHeights = n, y.maximumHeights = r, y.granularity = d, new u(y))
    }, u.fromConstantHeights = function (e) {
        var t, a, i = (e = Z.defaultValue(e, Z.defaultValue.EMPTY_OBJECT)).positions, n = e.minimumHeight,
            r = e.maximumHeight, o = Z.defined(n), s = Z.defined(r);
        if (o || s) {
            var m = i.length;
            t = o ? new Array(m) : void 0, a = s ? new Array(m) : void 0;
            for (var l = 0; l < m; ++l) o && (t[l] = n), s && (a[l] = r)
        }
        return new u({
            positions: i,
            maximumHeights: a,
            minimumHeights: t,
            ellipsoid: e.ellipsoid,
            vertexFormat: e.vertexFormat
        })
    }, u.createGeometry = function (e) {
        var t = e._positions, a = e._minimumHeights, i = e._maximumHeights, n = e._vertexFormat, r = e._granularity,
            o = e._ellipsoid, s = ae.WallGeometryLibrary.computePositions(o, t, i, a, r, !0);
        if (Z.defined(s)) {
            var m, l = s.bottomPositions, d = s.topPositions, p = s.numCorners, u = d.length, f = 2 * u,
                c = n.position ? new Float64Array(f) : void 0, y = n.normal ? new Float32Array(f) : void 0,
                g = n.tangent ? new Float32Array(f) : void 0, h = n.bitangent ? new Float32Array(f) : void 0,
                v = n.st ? new Float32Array(f / 3 * 2) : void 0, C = 0, A = 0, x = 0, b = 0, _ = 0, E = de, w = le,
                F = me, L = !0, k = 0, G = 1 / ((u /= 3) - t.length + 1);
            for (m = 0; m < u; ++m) {
                var P = 3 * m, H = K.Cartesian3.fromArray(d, P, ie), V = K.Cartesian3.fromArray(l, P, ne);
                if (n.position && (c[C++] = V.x, c[C++] = V.y, c[C++] = V.z, c[C++] = H.x, c[C++] = H.y, c[C++] = H.z), n.st && (v[_++] = k, v[_++] = 0, v[_++] = k, v[_++] = 1), n.normal || n.tangent || n.bitangent) {
                    var T, D = K.Cartesian3.clone(K.Cartesian3.ZERO, se),
                        z = o.scaleToGeodeticSurface(K.Cartesian3.fromArray(d, P, ne), ne);
                    if (m + 1 < u && (T = o.scaleToGeodeticSurface(K.Cartesian3.fromArray(d, 3 + P, re), re), D = K.Cartesian3.fromArray(d, 3 + P, se)), L) {
                        var O = K.Cartesian3.subtract(D, H, oe), S = K.Cartesian3.subtract(z, H, ie);
                        E = K.Cartesian3.normalize(K.Cartesian3.cross(S, O, E), E), L = !1
                    }
                    K.Cartesian3.equalsEpsilon(T, z, j.CesiumMath.EPSILON10) ? L = !0 : (k += G, n.tangent && (w = K.Cartesian3.normalize(K.Cartesian3.subtract(T, z, w), w)), n.bitangent && (F = K.Cartesian3.normalize(K.Cartesian3.cross(E, w, F), F))), n.normal && (y[A++] = E.x, y[A++] = E.y, y[A++] = E.z, y[A++] = E.x, y[A++] = E.y, y[A++] = E.z), n.tangent && (g[b++] = w.x, g[b++] = w.y, g[b++] = w.z, g[b++] = w.x, g[b++] = w.y, g[b++] = w.z), n.bitangent && (h[x++] = F.x, h[x++] = F.y, h[x++] = F.z, h[x++] = F.x, h[x++] = F.y, h[x++] = F.z)
                }
            }
            var I = new ee.GeometryAttributes;
            n.position && (I.position = new $.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: c
            })), n.normal && (I.normal = new $.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: y
            })), n.tangent && (I.tangent = new $.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: g
            })), n.bitangent && (I.bitangent = new $.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: h
            })), n.st && (I.st = new $.GeometryAttribute({
                componentDatatype: X.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: v
            }));
            var R = f / 3;
            f -= 6 * (p + 1);
            var M = te.IndexDatatype.createTypedArray(R, f), N = 0;
            for (m = 0; m < R - 2; m += 2) {
                var W = m, B = m + 2, U = K.Cartesian3.fromArray(c, 3 * W, ie),
                    q = K.Cartesian3.fromArray(c, 3 * B, ne);
                if (!K.Cartesian3.equalsEpsilon(U, q, j.CesiumMath.EPSILON10)) {
                    var J = m + 1, Y = m + 3;
                    M[N++] = J, M[N++] = W, M[N++] = Y, M[N++] = Y, M[N++] = W, M[N++] = B
                }
            }
            return new $.Geometry({
                attributes: I,
                indices: M,
                primitiveType: $.PrimitiveType.TRIANGLES,
                boundingSphere: new Q.BoundingSphere.fromVertices(c)
            })
        }
    }, function (e, t) {
        return Z.defined(t) && (e = u.unpack(e, t)), e._ellipsoid = K.Ellipsoid.clone(e._ellipsoid), u.createGeometry(e)
    }
});
