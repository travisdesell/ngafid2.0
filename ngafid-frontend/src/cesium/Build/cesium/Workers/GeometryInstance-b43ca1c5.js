define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Transforms-44592b02"], function (e, t, i, r) {
    "use strict";
    e.GeometryInstance = function (e) {
        e = t.defaultValue(e, t.defaultValue.EMPTY_OBJECT), this.geometry = e.geometry, this.modelMatrix = r.Matrix4.clone(t.defaultValue(e.modelMatrix, r.Matrix4.IDENTITY)), this.id = e.id, this.pickPrimitive = e.pickPrimitive, this.attributes = t.defaultValue(e.attributes, {}), this.westHemisphereGeometry = void 0, this.eastHemisphereGeometry = void 0
    }
});
