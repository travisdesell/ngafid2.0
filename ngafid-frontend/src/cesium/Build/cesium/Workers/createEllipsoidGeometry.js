define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./EllipsoidGeometry-3c001161"], function (r, e, t, n, o, d, i, a, f, c, s, m, u, y) {
    "use strict";
    return function (e, t) {
        return r.defined(t) && (e = y.EllipsoidGeometry.unpack(e, t)), y.EllipsoidGeometry.createGeometry(e)
    }
});
