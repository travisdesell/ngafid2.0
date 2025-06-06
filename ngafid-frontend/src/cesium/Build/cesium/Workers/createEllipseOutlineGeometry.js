define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./EllipseGeometryLibrary-de353ee5", "./EllipseOutlineGeometry-99f34d55"], function (r, e, t, n, i, o, l, s, a, d, c, f, u, m) {
    "use strict";
    return function (e, t) {
        return r.defined(t) && (e = m.EllipseOutlineGeometry.unpack(e, t)), e._center = n.Cartesian3.clone(e._center), e._ellipsoid = n.Ellipsoid.clone(e._ellipsoid), m.EllipseOutlineGeometry.createGeometry(e)
    }
});
