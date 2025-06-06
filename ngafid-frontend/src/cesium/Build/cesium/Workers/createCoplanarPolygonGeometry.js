define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./GeometryInstance-b43ca1c5", "./arrayRemoveDuplicates-a580a060", "./BoundingRectangle-8481a283", "./EllipsoidTangentPlane-30395e74", "./OrientedBoundingBox-e6450288", "./CoplanarPolygonGeometryLibrary-17afdb93", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolygonGeometryLibrary-0de16e21"], function (s, e, V, R, I, t, a, M, H, B, n, w, r, O, o, i, p, A, F, l, y, c, G, m, u, z, L) {
    "use strict";
    var S = new R.Cartesian3, E = new l.BoundingRectangle, N = new R.Cartesian2, Q = new R.Cartesian2,
        T = new R.Cartesian3, D = new R.Cartesian3, _ = new R.Cartesian3, k = new R.Cartesian3, j = new R.Cartesian3,
        U = new R.Cartesian3, Y = new I.Quaternion, q = new I.Matrix3, J = new I.Matrix3, W = new R.Cartesian3;

    function Z(e, t, a, n, r, o, i, l) {
        var s = e.positions, p = z.PolygonPipeline.triangulate(e.positions2D, e.holes);
        p.length < 3 && (p = [0, 1, 2]);
        var y = O.IndexDatatype.createTypedArray(s.length, p.length);
        y.set(p);
        var c = q;
        if (0 !== n) {
            var m = I.Quaternion.fromAxisAngle(o, n, Y);
            if (c = I.Matrix3.fromQuaternion(m, c), t.tangent || t.bitangent) {
                m = I.Quaternion.fromAxisAngle(o, -n, Y);
                var u = I.Matrix3.fromQuaternion(m, J);
                i = R.Cartesian3.normalize(I.Matrix3.multiplyByVector(u, i, i), i), t.bitangent && (l = R.Cartesian3.normalize(R.Cartesian3.cross(o, i, l), l))
            }
        } else c = I.Matrix3.clone(I.Matrix3.IDENTITY, c);
        var d = Q;
        t.st && (d.x = a.x, d.y = a.y);
        for (var g = s.length, v = 3 * g, b = new Float64Array(v), f = t.normal ? new Float32Array(v) : void 0, h = t.tangent ? new Float32Array(v) : void 0, C = t.bitangent ? new Float32Array(v) : void 0, x = t.st ? new Float32Array(2 * g) : void 0, P = 0, w = 0, A = 0, F = 0, G = 0, L = 0; L < g; L++) {
            var E = s[L];
            if (b[P++] = E.x, b[P++] = E.y, b[P++] = E.z, t.st) {
                var T = r(I.Matrix3.multiplyByVector(c, E, S), N);
                R.Cartesian2.subtract(T, d, T);
                var D = V.CesiumMath.clamp(T.x / a.width, 0, 1), _ = V.CesiumMath.clamp(T.y / a.height, 0, 1);
                x[G++] = D, x[G++] = _
            }
            t.normal && (f[w++] = o.x, f[w++] = o.y, f[w++] = o.z), t.tangent && (h[F++] = i.x, h[F++] = i.y, h[F++] = i.z), t.bitangent && (C[A++] = l.x, C[A++] = l.y, C[A++] = l.z)
        }
        var k = new B.GeometryAttributes;
        return t.position && (k.position = new H.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: b
        })), t.normal && (k.normal = new H.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: f
        })), t.tangent && (k.tangent = new H.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: h
        })), t.bitangent && (k.bitangent = new H.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: C
        })), t.st && (k.st = new H.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 2,
            values: x
        })), new H.Geometry({attributes: k, indices: y, primitiveType: H.PrimitiveType.TRIANGLES})
    }

    function d(e) {
        var t = (e = s.defaultValue(e, s.defaultValue.EMPTY_OBJECT)).polygonHierarchy,
            a = s.defaultValue(e.vertexFormat, p.VertexFormat.DEFAULT);
        this._vertexFormat = p.VertexFormat.clone(a), this._polygonHierarchy = t, this._stRotation = s.defaultValue(e.stRotation, 0), this._ellipsoid = R.Ellipsoid.clone(s.defaultValue(e.ellipsoid, R.Ellipsoid.WGS84)), this._workerName = "createCoplanarPolygonGeometry", this.packedLength = L.PolygonGeometryLibrary.computeHierarchyPackedLength(t) + p.VertexFormat.packedLength + R.Ellipsoid.packedLength + 2
    }

    d.fromPositions = function (e) {
        return new d({
            polygonHierarchy: {positions: (e = s.defaultValue(e, s.defaultValue.EMPTY_OBJECT)).positions},
            vertexFormat: e.vertexFormat,
            stRotation: e.stRotation,
            ellipsoid: e.ellipsoid
        })
    }, d.pack = function (e, t, a) {
        return a = s.defaultValue(a, 0), a = L.PolygonGeometryLibrary.packPolygonHierarchy(e._polygonHierarchy, t, a), R.Ellipsoid.pack(e._ellipsoid, t, a), a += R.Ellipsoid.packedLength, p.VertexFormat.pack(e._vertexFormat, t, a), a += p.VertexFormat.packedLength, t[a++] = e._stRotation, t[a] = e.packedLength, t
    };
    var g = R.Ellipsoid.clone(R.Ellipsoid.UNIT_SPHERE), v = new p.VertexFormat, b = {polygonHierarchy: {}};
    return d.unpack = function (e, t, a) {
        t = s.defaultValue(t, 0);
        var n = L.PolygonGeometryLibrary.unpackPolygonHierarchy(e, t);
        t = n.startingIndex, delete n.startingIndex;
        var r = R.Ellipsoid.unpack(e, t, g);
        t += R.Ellipsoid.packedLength;
        var o = p.VertexFormat.unpack(e, t, v);
        t += p.VertexFormat.packedLength;
        var i = e[t++], l = e[t];
        return s.defined(a) || (a = new d(b)), a._polygonHierarchy = n, a._ellipsoid = R.Ellipsoid.clone(r, a._ellipsoid), a._vertexFormat = p.VertexFormat.clone(o, a._vertexFormat), a._stRotation = i, a.packedLength = l, a
    }, d.createGeometry = function (e) {
        var t = e._vertexFormat, a = e._polygonHierarchy, n = e._stRotation, r = a.positions;
        if (!((r = F.arrayRemoveDuplicates(r, R.Cartesian3.equalsEpsilon, !0)).length < 3)) {
            var o = T, i = D, l = _, s = j, p = U;
            if (G.CoplanarPolygonGeometryLibrary.computeProjectTo2DArguments(r, k, s, p)) {
                if (o = R.Cartesian3.cross(s, p, o), o = R.Cartesian3.normalize(o, o), !R.Cartesian3.equalsEpsilon(k, R.Cartesian3.ZERO, V.CesiumMath.EPSILON6)) {
                    var y = e._ellipsoid.geodeticSurfaceNormal(k, W);
                    R.Cartesian3.dot(o, y) < 0 && (o = R.Cartesian3.negate(o, o), s = R.Cartesian3.negate(s, s))
                }
                var c = G.CoplanarPolygonGeometryLibrary.createProjectPointsTo2DFunction(k, s, p),
                    m = G.CoplanarPolygonGeometryLibrary.createProjectPointTo2DFunction(k, s, p);
                t.tangent && (i = R.Cartesian3.clone(s, i)), t.bitangent && (l = R.Cartesian3.clone(p, l));
                var u = L.PolygonGeometryLibrary.polygonsFromHierarchy(a, c, !1), d = u.hierarchy, g = u.polygons;
                if (0 !== d.length) {
                    r = d[0].outerRing;
                    for (var v = I.BoundingSphere.fromPoints(r), b = L.PolygonGeometryLibrary.computeBoundingRectangle(o, m, r, n, E), f = [], h = 0; h < g.length; h++) {
                        var C = new A.GeometryInstance({geometry: Z(g[h], t, b, n, m, o, i, l)});
                        f.push(C)
                    }
                    var x = w.GeometryPipeline.combineInstances(f)[0];
                    x.attributes.position.values = new Float64Array(x.attributes.position.values), x.indices = O.IndexDatatype.createTypedArray(x.attributes.position.values.length / 3, x.indices);
                    var P = x.attributes;
                    return t.position || delete P.position, new H.Geometry({
                        attributes: P,
                        indices: x.indices,
                        primitiveType: x.primitiveType,
                        boundingSphere: v
                    })
                }
            }
        }
    }, function (e, t) {
        return s.defined(t) && (e = d.unpack(e, t)), d.createGeometry(e)
    }
});
