define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./arrayRemoveDuplicates-a580a060", "./BoundingRectangle-8481a283", "./EllipsoidTangentPlane-30395e74", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolylineVolumeGeometryLibrary-6dcd44cb", "./EllipsoidGeodesic-19bdf744", "./PolylinePipeline-3852f7d2"], function (d, e, a, u, y, i, n, h, f, g, m, t, r, o, l, s, p, c, v, E, P) {
    "use strict";

    function _(e) {
        var i = (e = d.defaultValue(e, d.defaultValue.EMPTY_OBJECT)).polylinePositions, n = e.shapePositions;
        this._positions = i, this._shape = n, this._ellipsoid = u.Ellipsoid.clone(d.defaultValue(e.ellipsoid, u.Ellipsoid.WGS84)), this._cornerType = d.defaultValue(e.cornerType, v.CornerType.ROUNDED), this._granularity = d.defaultValue(e.granularity, a.CesiumMath.RADIANS_PER_DEGREE), this._workerName = "createPolylineVolumeOutlineGeometry";
        var t = 1 + i.length * u.Cartesian3.packedLength;
        t += 1 + n.length * u.Cartesian2.packedLength, this.packedLength = t + u.Ellipsoid.packedLength + 2
    }

    _.pack = function (e, i, n) {
        var t;
        n = d.defaultValue(n, 0);
        var a = e._positions, r = a.length;
        for (i[n++] = r, t = 0; t < r; ++t, n += u.Cartesian3.packedLength) u.Cartesian3.pack(a[t], i, n);
        var o = e._shape;
        for (r = o.length, i[n++] = r, t = 0; t < r; ++t, n += u.Cartesian2.packedLength) u.Cartesian2.pack(o[t], i, n);
        return u.Ellipsoid.pack(e._ellipsoid, i, n), n += u.Ellipsoid.packedLength, i[n++] = e._cornerType, i[n] = e._granularity, i
    };
    var k = u.Ellipsoid.clone(u.Ellipsoid.UNIT_SPHERE), C = {
        polylinePositions: void 0,
        shapePositions: void 0,
        ellipsoid: k,
        height: void 0,
        cornerType: void 0,
        granularity: void 0
    };
    _.unpack = function (e, i, n) {
        var t;
        i = d.defaultValue(i, 0);
        var a = e[i++], r = new Array(a);
        for (t = 0; t < a; ++t, i += u.Cartesian3.packedLength) r[t] = u.Cartesian3.unpack(e, i);
        a = e[i++];
        var o = new Array(a);
        for (t = 0; t < a; ++t, i += u.Cartesian2.packedLength) o[t] = u.Cartesian2.unpack(e, i);
        var l = u.Ellipsoid.unpack(e, i, k);
        i += u.Ellipsoid.packedLength;
        var s = e[i++], p = e[i];
        return d.defined(n) ? (n._positions = r, n._shape = o, n._ellipsoid = u.Ellipsoid.clone(l, n._ellipsoid), n._cornerType = s, n._granularity = p, n) : (C.polylinePositions = r, C.shapePositions = o, C.cornerType = s, C.granularity = p, new _(C))
    };
    var L = new l.BoundingRectangle;
    return _.createGeometry = function (e) {
        var i = e._positions, n = o.arrayRemoveDuplicates(i, u.Cartesian3.equalsEpsilon), t = e._shape;
        if (t = v.PolylineVolumeGeometryLibrary.removeDuplicatesFromShape(t), !(n.length < 2 || t.length < 3)) {
            c.PolygonPipeline.computeWindingOrder2D(t) === c.WindingOrder.CLOCKWISE && t.reverse();
            var a = l.BoundingRectangle.fromPoints(t, L);
            return function (e, i) {
                var n = new g.GeometryAttributes;
                n.position = new f.GeometryAttribute({
                    componentDatatype: h.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: e
                });
                var t, a, r = i.length, o = n.position.values.length / 3, l = e.length / 3 / r,
                    s = m.IndexDatatype.createTypedArray(o, 2 * r * (1 + l)), p = 0, d = (t = 0) * r;
                for (a = 0; a < r - 1; a++) s[p++] = a + d, s[p++] = a + d + 1;
                for (s[p++] = r - 1 + d, s[p++] = d, d = (t = l - 1) * r, a = 0; a < r - 1; a++) s[p++] = a + d, s[p++] = a + d + 1;
                for (s[p++] = r - 1 + d, s[p++] = d, t = 0; t < l - 1; t++) {
                    var u = r * t, c = u + r;
                    for (a = 0; a < r; a++) s[p++] = a + u, s[p++] = a + c
                }
                return new f.Geometry({
                    attributes: n,
                    indices: m.IndexDatatype.createTypedArray(o, s),
                    boundingSphere: y.BoundingSphere.fromVertices(e),
                    primitiveType: f.PrimitiveType.LINES
                })
            }(v.PolylineVolumeGeometryLibrary.computePositions(n, t, a, e, !1), t)
        }
    }, function (e, i) {
        return d.defined(i) && (e = _.unpack(e, i)), e._ellipsoid = u.Ellipsoid.clone(e._ellipsoid), _.createGeometry(e)
    }
});
