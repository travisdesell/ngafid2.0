define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryInstance-b43ca1c5", "./arrayRemoveDuplicates-a580a060", "./EllipsoidTangentPlane-30395e74", "./OrientedBoundingBox-e6450288", "./CoplanarPolygonGeometryLibrary-17afdb93", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolygonGeometryLibrary-0de16e21"], function (a, e, t, c, p, r, n, s, u, d, o, m, i, f, y, l, g, b, h, P, G, v, L, C, T) {
    "use strict";

    function E(e) {
        for (var t = e.length, r = new Float64Array(3 * t), n = f.IndexDatatype.createTypedArray(t, 2 * t), o = 0, a = 0, i = 0; i < t; i++) {
            var y = e[i];
            r[o++] = y.x, r[o++] = y.y, r[o++] = y.z, n[a++] = i, n[a++] = (i + 1) % t
        }
        var l = new d.GeometryAttributes({
            position: new u.GeometryAttribute({
                componentDatatype: s.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: r
            })
        });
        return new u.Geometry({attributes: l, indices: n, primitiveType: u.PrimitiveType.LINES})
    }

    function k(e) {
        var t = (e = a.defaultValue(e, a.defaultValue.EMPTY_OBJECT)).polygonHierarchy;
        this._polygonHierarchy = t, this._workerName = "createCoplanarPolygonOutlineGeometry", this.packedLength = T.PolygonGeometryLibrary.computeHierarchyPackedLength(t) + 1
    }

    k.fromPositions = function (e) {
        return new k({polygonHierarchy: {positions: (e = a.defaultValue(e, a.defaultValue.EMPTY_OBJECT)).positions}})
    }, k.pack = function (e, t, r) {
        return r = a.defaultValue(r, 0), t[r = T.PolygonGeometryLibrary.packPolygonHierarchy(e._polygonHierarchy, t, r)] = e.packedLength, t
    };
    var H = {polygonHierarchy: {}};
    return k.unpack = function (e, t, r) {
        t = a.defaultValue(t, 0);
        var n = T.PolygonGeometryLibrary.unpackPolygonHierarchy(e, t);
        t = n.startingIndex, delete n.startingIndex;
        var o = e[t];
        return a.defined(r) || (r = new k(H)), r._polygonHierarchy = n, r.packedLength = o, r
    }, k.createGeometry = function (e) {
        var t = e._polygonHierarchy, r = t.positions;
        if (!((r = b.arrayRemoveDuplicates(r, c.Cartesian3.equalsEpsilon, !0)).length < 3) && G.CoplanarPolygonGeometryLibrary.validOutline(r)) {
            var n = T.PolygonGeometryLibrary.polygonOutlinesFromHierarchy(t, !1);
            if (0 !== n.length) {
                for (var o = [], a = 0; a < n.length; a++) {
                    var i = new g.GeometryInstance({geometry: E(n[a])});
                    o.push(i)
                }
                var y = m.GeometryPipeline.combineInstances(o)[0], l = p.BoundingSphere.fromPoints(t.positions);
                return new u.Geometry({
                    attributes: y.attributes,
                    indices: y.indices,
                    primitiveType: y.primitiveType,
                    boundingSphere: l
                })
            }
        }
    }, function (e, t) {
        return a.defined(t) && (e = k.unpack(e, t)), e._ellipsoid = c.Ellipsoid.clone(e._ellipsoid), k.createGeometry(e)
    }
});
