define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./Plane-2d882f9f", "./VertexFormat-ad523db1", "./FrustumGeometry-9dd09ba5"], function (r, e, t, n, a, d, u, o, f, m, s, c, i) {
    "use strict";
    return function (e, t) {
        return r.defined(t) && (e = i.FrustumGeometry.unpack(e, t)), i.FrustumGeometry.createGeometry(e)
    }
});
