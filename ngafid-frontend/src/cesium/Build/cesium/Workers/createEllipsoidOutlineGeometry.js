define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./EllipsoidOutlineGeometry-34ee67fd"], function (r, e, t, n, i, o, f, d, u, a, s, c, m) {
    "use strict";
    return function (e, t) {
        return r.defined(e.buffer) && (e = m.EllipsoidOutlineGeometry.unpack(e, t)), m.EllipsoidOutlineGeometry.createGeometry(e)
    }
});
