define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./PrimitivePipeline-a7956312", "./WebMercatorProjection-44bf888f", "./createTaskProcessorWorker"], function (e, t, i, r, n, o, a, s, c, m, b, f, d, P, p, u, y, C, l) {
    "use strict";
    return l(function (e, t) {
        var i = y.PrimitivePipeline.unpackCombineGeometryParameters(e), r = y.PrimitivePipeline.combineGeometry(i);
        return y.PrimitivePipeline.packCombineGeometryResults(r, t)
    })
});
