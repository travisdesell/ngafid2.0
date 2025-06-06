define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./FrustumGeometry-9dd09ba5"], function (c, e, t, p, f, r, n, h, g, _, a, u, k) {
    "use strict";
    var m = 0, o = 1;

    function d(e) {
        var t, r, n = e.frustum, a = e.orientation, u = e.origin, i = c.defaultValue(e._drawNearPlane, !0);
        n instanceof k.PerspectiveFrustum ? (t = m, r = k.PerspectiveFrustum.packedLength) : n instanceof k.OrthographicFrustum && (t = o, r = k.OrthographicFrustum.packedLength), this._frustumType = t, this._frustum = n.clone(), this._origin = p.Cartesian3.clone(u), this._orientation = f.Quaternion.clone(a), this._drawNearPlane = i, this._workerName = "createFrustumOutlineGeometry", this.packedLength = 2 + r + p.Cartesian3.packedLength + f.Quaternion.packedLength
    }

    d.pack = function (e, t, r) {
        r = c.defaultValue(r, 0);
        var n = e._frustumType, a = e._frustum;
        return (t[r++] = n) === m ? (k.PerspectiveFrustum.pack(a, t, r), r += k.PerspectiveFrustum.packedLength) : (k.OrthographicFrustum.pack(a, t, r), r += k.OrthographicFrustum.packedLength), p.Cartesian3.pack(e._origin, t, r), r += p.Cartesian3.packedLength, f.Quaternion.pack(e._orientation, t, r), t[r += f.Quaternion.packedLength] = e._drawNearPlane ? 1 : 0, t
    };
    var l = new k.PerspectiveFrustum, y = new k.OrthographicFrustum, v = new f.Quaternion, F = new p.Cartesian3;
    return d.unpack = function (e, t, r) {
        t = c.defaultValue(t, 0);
        var n, a = e[t++];
        a === m ? (n = k.PerspectiveFrustum.unpack(e, t, l), t += k.PerspectiveFrustum.packedLength) : (n = k.OrthographicFrustum.unpack(e, t, y), t += k.OrthographicFrustum.packedLength);
        var u = p.Cartesian3.unpack(e, t, F);
        t += p.Cartesian3.packedLength;
        var i = f.Quaternion.unpack(e, t, v), o = 1 === e[t += f.Quaternion.packedLength];
        if (!c.defined(r)) return new d({frustum: n, origin: u, orientation: i, _drawNearPlane: o});
        var s = a === r._frustumType ? r._frustum : void 0;
        return r._frustum = n.clone(s), r._frustumType = a, r._origin = p.Cartesian3.clone(u, r._origin), r._orientation = f.Quaternion.clone(i, r._orientation), r._drawNearPlane = o, r
    }, d.createGeometry = function (e) {
        var t = e._frustumType, r = e._frustum, n = e._origin, a = e._orientation, u = e._drawNearPlane,
            i = new Float64Array(24);
        k.FrustumGeometry._computeNearFarPlanes(n, a, t, r, i);
        for (var o, s, c = new _.GeometryAttributes({
            position: new g.GeometryAttribute({
                componentDatatype: h.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: i
            })
        }), p = u ? 2 : 1, m = new Uint16Array(8 * (1 + p)), d = u ? 0 : 1; d < 2; ++d) s = 4 * d, m[o = u ? 8 * d : 0] = s, m[o + 1] = s + 1, m[o + 2] = s + 1, m[o + 3] = s + 2, m[o + 4] = s + 2, m[o + 5] = s + 3, m[o + 6] = s + 3, m[o + 7] = s;
        for (d = 0; d < 2; ++d) s = 4 * d, m[o = 8 * (p + d)] = s, m[o + 1] = s + 4, m[o + 2] = s + 1, m[o + 3] = s + 5, m[o + 4] = s + 2, m[o + 5] = s + 6, m[o + 6] = s + 3, m[o + 7] = s + 7;
        return new g.Geometry({
            attributes: c,
            indices: m,
            primitiveType: g.PrimitiveType.LINES,
            boundingSphere: f.BoundingSphere.fromVertices(i)
        })
    }, function (e, t) {
        return c.defined(t) && (e = d.unpack(e, t)), d.createGeometry(e)
    }
});
