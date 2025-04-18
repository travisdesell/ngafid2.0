define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc"], function (r, e, t, a, i, n, o, u, c, s) {
    "use strict";

    function y() {
        this._workerName = "createPlaneOutlineGeometry"
    }

    y.packedLength = 0, y.pack = function (e, t) {
        return t
    }, y.unpack = function (e, t, n) {
        return r.defined(n) ? n : new y
    };
    var m = new a.Cartesian3(-.5, -.5, 0), p = new a.Cartesian3(.5, .5, 0);
    return y.createGeometry = function () {
        var e = new s.GeometryAttributes, t = new Uint16Array(8), n = new Float64Array(12);
        return n[0] = m.x, n[1] = m.y, n[2] = m.z, n[3] = p.x, n[4] = m.y, n[5] = m.z, n[6] = p.x, n[7] = p.y, n[8] = m.z, n[9] = m.x, n[10] = p.y, n[11] = m.z, e.position = new c.GeometryAttribute({
            componentDatatype: u.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: n
        }), t[0] = 0, t[1] = 1, t[2] = 1, t[3] = 2, t[4] = 2, t[5] = 3, t[6] = 3, t[7] = 0, new c.Geometry({
            attributes: e,
            indices: t,
            primitiveType: c.PrimitiveType.LINES,
            boundingSphere: new i.BoundingSphere(a.Cartesian3.ZERO, Math.sqrt(2))
        })
    }, function (e, t) {
        return r.defined(t) && (e = y.unpack(e, t)), y.createGeometry(e)
    }
});
