define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./EllipseGeometryLibrary-de353ee5", "./GeometryInstance-b43ca1c5", "./EllipseGeometry-7d13646a"], function (r, e, t, n, i, o, a, d, s, c, l, f, b, m, p, y, u, G, C, E, A) {
    "use strict";
    return function (e, t) {
        return r.defined(t) && (e = A.EllipseGeometry.unpack(e, t)), e._center = n.Cartesian3.clone(e._center), e._ellipsoid = n.Ellipsoid.clone(e._ellipsoid), A.EllipseGeometry.createGeometry(e)
    }
});
