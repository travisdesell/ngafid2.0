define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./arrayRemoveDuplicates-a580a060", "./BoundingRectangle-8481a283", "./EllipsoidTangentPlane-30395e74", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolylineVolumeGeometryLibrary-6dcd44cb", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2"], function (u, e, i, c, G, t, n, A, R, D, r, I, a, O, o, l, g, s, d, p, y, S, m, h, f) {
    "use strict";
    var v = {};

    function B(e, t) {
        u.defined(v[e]) || (v[e] = !0, console.warn(u.defaultValue(t, e)))
    }

    function b(e) {
        var t = (e = u.defaultValue(e, u.defaultValue.EMPTY_OBJECT)).polylinePositions, n = e.shapePositions;
        this._positions = t, this._shape = n, this._ellipsoid = c.Ellipsoid.clone(u.defaultValue(e.ellipsoid, c.Ellipsoid.WGS84)), this._cornerType = u.defaultValue(e.cornerType, m.CornerType.ROUNDED), this._vertexFormat = g.VertexFormat.clone(u.defaultValue(e.vertexFormat, g.VertexFormat.DEFAULT)), this._granularity = u.defaultValue(e.granularity, i.CesiumMath.RADIANS_PER_DEGREE), this._workerName = "createPolylineVolumeGeometry";
        var r = 1 + t.length * c.Cartesian3.packedLength;
        r += 1 + n.length * c.Cartesian2.packedLength, this.packedLength = r + c.Ellipsoid.packedLength + g.VertexFormat.packedLength + 2
    }

    B.geometryOutlines = "Entity geometry outlines are unsupported on terrain. Outlines will be disabled. To enable outlines, disable geometry terrain clamping by explicitly setting height to 0.", B.geometryZIndex = "Entity geometry with zIndex are unsupported when height or extrudedHeight are defined.  zIndex will be ignored", B.geometryHeightReference = "Entity corridor, ellipse, polygon or rectangle with heightReference must also have a defined height.  heightReference will be ignored", B.geometryExtrudedHeightReference = "Entity corridor, ellipse, polygon or rectangle with extrudedHeightReference must also have a defined extrudedHeight.  extrudedHeightReference will be ignored", b.pack = function (e, t, n) {
        var r;
        n = u.defaultValue(n, 0);
        var i = e._positions, a = i.length;
        for (t[n++] = a, r = 0; r < a; ++r, n += c.Cartesian3.packedLength) c.Cartesian3.pack(i[r], t, n);
        var o = e._shape;
        for (a = o.length, t[n++] = a, r = 0; r < a; ++r, n += c.Cartesian2.packedLength) c.Cartesian2.pack(o[r], t, n);
        return c.Ellipsoid.pack(e._ellipsoid, t, n), n += c.Ellipsoid.packedLength, g.VertexFormat.pack(e._vertexFormat, t, n), n += g.VertexFormat.packedLength, t[n++] = e._cornerType, t[n] = e._granularity, t
    };
    var E = c.Ellipsoid.clone(c.Ellipsoid.UNIT_SPHERE), P = new g.VertexFormat, _ = {
        polylinePositions: void 0,
        shapePositions: void 0,
        ellipsoid: E,
        vertexFormat: P,
        cornerType: void 0,
        granularity: void 0
    };
    b.unpack = function (e, t, n) {
        var r;
        t = u.defaultValue(t, 0);
        var i = e[t++], a = new Array(i);
        for (r = 0; r < i; ++r, t += c.Cartesian3.packedLength) a[r] = c.Cartesian3.unpack(e, t);
        i = e[t++];
        var o = new Array(i);
        for (r = 0; r < i; ++r, t += c.Cartesian2.packedLength) o[r] = c.Cartesian2.unpack(e, t);
        var l = c.Ellipsoid.unpack(e, t, E);
        t += c.Ellipsoid.packedLength;
        var s = g.VertexFormat.unpack(e, t, P);
        t += g.VertexFormat.packedLength;
        var d = e[t++], p = e[t];
        return u.defined(n) ? (n._positions = a, n._shape = o, n._ellipsoid = c.Ellipsoid.clone(l, n._ellipsoid), n._vertexFormat = g.VertexFormat.clone(s, n._vertexFormat), n._cornerType = d, n._granularity = p, n) : (_.polylinePositions = a, _.shapePositions = o, _.cornerType = d, _.granularity = p, new b(_))
    };
    var x = new d.BoundingRectangle;
    return b.createGeometry = function (e) {
        var t = e._positions, n = s.arrayRemoveDuplicates(t, c.Cartesian3.equalsEpsilon), r = e._shape;
        if (r = m.PolylineVolumeGeometryLibrary.removeDuplicatesFromShape(r), !(n.length < 2 || r.length < 3)) {
            S.PolygonPipeline.computeWindingOrder2D(r) === S.WindingOrder.CLOCKWISE && r.reverse();
            var i = d.BoundingRectangle.fromPoints(r, x);
            return function (e, t, n, r) {
                var i = new D.GeometryAttributes;
                r.position && (i.position = new R.GeometryAttribute({
                    componentDatatype: A.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: e
                }));
                var a, o, l, s, d, p, u = t.length, c = e.length / 3, g = (c - 2 * u) / (2 * u),
                    y = S.PolygonPipeline.triangulate(t), m = (g - 1) * u * 6 + 2 * y.length,
                    h = O.IndexDatatype.createTypedArray(c, m), f = 2 * u, v = 0;
                for (a = 0; a < g - 1; a++) {
                    for (o = 0; o < u - 1; o++) p = (l = 2 * o + a * u * 2) + f, d = (s = l + 1) + f, h[v++] = s, h[v++] = l, h[v++] = d, h[v++] = d, h[v++] = l, h[v++] = p;
                    d = (s = (l = 2 * u - 2 + a * u * 2) + 1) + f, p = l + f, h[v++] = s, h[v++] = l, h[v++] = d, h[v++] = d, h[v++] = l, h[v++] = p
                }
                if (r.st || r.tangent || r.bitangent) {
                    var b, E, P = new Float32Array(2 * c), _ = 1 / (g - 1), x = 1 / n.height, k = n.height / 2, C = 0;
                    for (a = 0; a < g; a++) {
                        for (b = a * _, E = x * (t[0].y + k), P[C++] = b, P[C++] = E, o = 1; o < u; o++) E = x * (t[o].y + k), P[C++] = b, P[C++] = E, P[C++] = b, P[C++] = E;
                        E = x * (t[0].y + k), P[C++] = b, P[C++] = E
                    }
                    for (o = 0; o < u; o++) b = 0, E = x * (t[o].y + k), P[C++] = b, P[C++] = E;
                    for (o = 0; o < u; o++) b = (g - 1) * _, E = x * (t[o].y + k), P[C++] = b, P[C++] = E;
                    i.st = new R.GeometryAttribute({
                        componentDatatype: A.ComponentDatatype.FLOAT,
                        componentsPerAttribute: 2,
                        values: new Float32Array(P)
                    })
                }
                var V = c - 2 * u;
                for (a = 0; a < y.length; a += 3) {
                    var L = y[a] + V, w = y[a + 1] + V, F = y[a + 2] + V;
                    h[v++] = L, h[v++] = w, h[v++] = F, h[v++] = F + u, h[v++] = w + u, h[v++] = L + u
                }
                var T = new R.Geometry({
                    attributes: i,
                    indices: h,
                    boundingSphere: G.BoundingSphere.fromVertices(e),
                    primitiveType: R.PrimitiveType.TRIANGLES
                });
                if (r.normal && (T = I.GeometryPipeline.computeNormal(T)), r.tangent || r.bitangent) {
                    try {
                        T = I.GeometryPipeline.computeTangentAndBitangent(T)
                    } catch (e) {
                        B("polyline-volume-tangent-bitangent", "Unable to compute tangents and bitangents for polyline volume geometry")
                    }
                    r.tangent || (T.attributes.tangent = void 0), r.bitangent || (T.attributes.bitangent = void 0), r.st || (T.attributes.st = void 0)
                }
                return T
            }(m.PolylineVolumeGeometryLibrary.computePositions(n, r, i, e, !0), r, i, e._vertexFormat)
        }
    }, function (e, t) {
        return u.defined(t) && (e = b.unpack(e, t)), e._ellipsoid = c.Ellipsoid.clone(e._ellipsoid), b.createGeometry(e)
    }
});
