define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./BoxGeometry-6bbe7a89"], function (r, e, t, o, n, a, f, d, c, m, b, i, u) {
    "use strict";
    return function (e, t) {
        return r.defined(t) && (e = u.BoxGeometry.unpack(e, t)), u.BoxGeometry.createGeometry(e)
    }
});
