define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryOffsetAttribute-e6e9672c", "./GeometryInstance-b43ca1c5", "./arrayRemoveDuplicates-a580a060", "./EllipsoidTangentPlane-30395e74", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolygonGeometryLibrary-0de16e21"], function (v, e, E, f, A, t, i, _, G, L, r, T, o, H, n, a, C, O, l, D, x, s, I, w) {
    "use strict";
    var S = [], k = [];

    function R(e, t, i, r, o) {
        var n, a, l = D.EllipsoidTangentPlane.fromPoints(t, e).projectPointsOntoPlane(t, S);
        I.PolygonPipeline.computeWindingOrder2D(l) === I.WindingOrder.CLOCKWISE && (l.reverse(), t = t.slice().reverse());
        var s = t.length, y = 0;
        if (r) for (n = new Float64Array(2 * s * 3), a = 0; a < s; a++) {
            var u = t[a], p = t[(a + 1) % s];
            n[y++] = u.x, n[y++] = u.y, n[y++] = u.z, n[y++] = p.x, n[y++] = p.y, n[y++] = p.z
        } else {
            var d = 0;
            if (o === x.ArcType.GEODESIC) for (a = 0; a < s; a++) d += w.PolygonGeometryLibrary.subdivideLineCount(t[a], t[(a + 1) % s], i); else if (o === x.ArcType.RHUMB) for (a = 0; a < s; a++) d += w.PolygonGeometryLibrary.subdivideRhumbLineCount(e, t[a], t[(a + 1) % s], i);
            for (n = new Float64Array(3 * d), a = 0; a < s; a++) {
                var f;
                o === x.ArcType.GEODESIC ? f = w.PolygonGeometryLibrary.subdivideLine(t[a], t[(a + 1) % s], i, k) : o === x.ArcType.RHUMB && (f = w.PolygonGeometryLibrary.subdivideRhumbLine(e, t[a], t[(a + 1) % s], i, k));
                for (var c = f.length, g = 0; g < c; ++g) n[y++] = f[g]
            }
        }
        var h = 2 * (s = n.length / 3), m = H.IndexDatatype.createTypedArray(s, h);
        for (a = y = 0; a < s - 1; a++) m[y++] = a, m[y++] = a + 1;
        return m[y++] = s - 1, m[y++] = 0, new O.GeometryInstance({
            geometry: new G.Geometry({
                attributes: new L.GeometryAttributes({
                    position: new G.GeometryAttribute({
                        componentDatatype: _.ComponentDatatype.DOUBLE,
                        componentsPerAttribute: 3,
                        values: n
                    })
                }), indices: m, primitiveType: G.PrimitiveType.LINES
            })
        })
    }

    function N(e, t, i, r, o) {
        var n, a, l = D.EllipsoidTangentPlane.fromPoints(t, e).projectPointsOntoPlane(t, S);
        I.PolygonPipeline.computeWindingOrder2D(l) === I.WindingOrder.CLOCKWISE && (l.reverse(), t = t.slice().reverse());
        var s = t.length, y = new Array(s), u = 0;
        if (r) for (n = new Float64Array(2 * s * 3 * 2), a = 0; a < s; ++a) {
            y[a] = u / 3;
            var p = t[a], d = t[(a + 1) % s];
            n[u++] = p.x, n[u++] = p.y, n[u++] = p.z, n[u++] = d.x, n[u++] = d.y, n[u++] = d.z
        } else {
            var f = 0;
            if (o === x.ArcType.GEODESIC) for (a = 0; a < s; a++) f += w.PolygonGeometryLibrary.subdivideLineCount(t[a], t[(a + 1) % s], i); else if (o === x.ArcType.RHUMB) for (a = 0; a < s; a++) f += w.PolygonGeometryLibrary.subdivideRhumbLineCount(e, t[a], t[(a + 1) % s], i);
            for (n = new Float64Array(3 * f * 2), a = 0; a < s; ++a) {
                var c;
                y[a] = u / 3, o === x.ArcType.GEODESIC ? c = w.PolygonGeometryLibrary.subdivideLine(t[a], t[(a + 1) % s], i, k) : o === x.ArcType.RHUMB && (c = w.PolygonGeometryLibrary.subdivideRhumbLine(e, t[a], t[(a + 1) % s], i, k));
                for (var g = c.length, h = 0; h < g; ++h) n[u++] = c[h]
            }
        }
        s = n.length / 6;
        var m = y.length, b = 2 * (2 * s + m), P = H.IndexDatatype.createTypedArray(s + m, b);
        for (a = u = 0; a < s; ++a) P[u++] = a, P[u++] = (a + 1) % s, P[u++] = a + s, P[u++] = (a + 1) % s + s;
        for (a = 0; a < m; a++) {
            var v = y[a];
            P[u++] = v, P[u++] = v + s
        }
        return new O.GeometryInstance({
            geometry: new G.Geometry({
                attributes: new L.GeometryAttributes({
                    position: new G.GeometryAttribute({
                        componentDatatype: _.ComponentDatatype.DOUBLE,
                        componentsPerAttribute: 3,
                        values: n
                    })
                }), indices: P, primitiveType: G.PrimitiveType.LINES
            })
        })
    }

    function c(e) {
        var t = e.polygonHierarchy, i = v.defaultValue(e.ellipsoid, f.Ellipsoid.WGS84),
            r = v.defaultValue(e.granularity, E.CesiumMath.RADIANS_PER_DEGREE),
            o = v.defaultValue(e.perPositionHeight, !1), n = o && v.defined(e.extrudedHeight),
            a = v.defaultValue(e.arcType, x.ArcType.GEODESIC), l = v.defaultValue(e.height, 0),
            s = v.defaultValue(e.extrudedHeight, l);
        if (!n) {
            var y = Math.max(l, s);
            s = Math.min(l, s), l = y
        }
        this._ellipsoid = f.Ellipsoid.clone(i), this._granularity = r, this._height = l, this._extrudedHeight = s, this._arcType = a, this._polygonHierarchy = t, this._perPositionHeight = o, this._perPositionHeightExtrude = n, this._offsetAttribute = e.offsetAttribute, this._workerName = "createPolygonOutlineGeometry", this.packedLength = w.PolygonGeometryLibrary.computeHierarchyPackedLength(t) + f.Ellipsoid.packedLength + 8
    }

    c.pack = function (e, t, i) {
        return i = v.defaultValue(i, 0), i = w.PolygonGeometryLibrary.packPolygonHierarchy(e._polygonHierarchy, t, i), f.Ellipsoid.pack(e._ellipsoid, t, i), i += f.Ellipsoid.packedLength, t[i++] = e._height, t[i++] = e._extrudedHeight, t[i++] = e._granularity, t[i++] = e._perPositionHeightExtrude ? 1 : 0, t[i++] = e._perPositionHeight ? 1 : 0, t[i++] = e._arcType, t[i++] = v.defaultValue(e._offsetAttribute, -1), t[i] = e.packedLength, t
    };
    var g = f.Ellipsoid.clone(f.Ellipsoid.UNIT_SPHERE), h = {polygonHierarchy: {}};
    return c.unpack = function (e, t, i) {
        t = v.defaultValue(t, 0);
        var r = w.PolygonGeometryLibrary.unpackPolygonHierarchy(e, t);
        t = r.startingIndex, delete r.startingIndex;
        var o = f.Ellipsoid.unpack(e, t, g);
        t += f.Ellipsoid.packedLength;
        var n = e[t++], a = e[t++], l = e[t++], s = 1 === e[t++], y = 1 === e[t++], u = e[t++], p = e[t++], d = e[t];
        return v.defined(i) || (i = new c(h)), i._polygonHierarchy = r, i._ellipsoid = f.Ellipsoid.clone(o, i._ellipsoid), i._height = n, i._extrudedHeight = a, i._granularity = l, i._perPositionHeight = y, i._perPositionHeightExtrude = s, i._arcType = u, i._offsetAttribute = -1 === p ? void 0 : p, i.packedLength = d, i
    }, c.fromPositions = function (e) {
        return new c({
            polygonHierarchy: {positions: (e = v.defaultValue(e, v.defaultValue.EMPTY_OBJECT)).positions},
            height: e.height,
            extrudedHeight: e.extrudedHeight,
            ellipsoid: e.ellipsoid,
            granularity: e.granularity,
            perPositionHeight: e.perPositionHeight,
            arcType: e.arcType,
            offsetAttribute: e.offsetAttribute
        })
    }, c.createGeometry = function (e) {
        var t = e._ellipsoid, i = e._granularity, r = e._polygonHierarchy, o = e._perPositionHeight, n = e._arcType,
            a = w.PolygonGeometryLibrary.polygonOutlinesFromHierarchy(r, !o, t);
        if (0 !== a.length) {
            var l, s, y, u = [], p = E.CesiumMath.chordLength(i, t.maximumRadius), d = e._height, f = e._extrudedHeight;
            if (e._perPositionHeightExtrude || !E.CesiumMath.equalsEpsilon(d, f, 0, E.CesiumMath.EPSILON2)) for (y = 0; y < a.length; y++) {
                if ((l = N(t, a[y], p, o, n)).geometry = w.PolygonGeometryLibrary.scaleToGeodeticHeightExtruded(l.geometry, d, f, t, o), v.defined(e._offsetAttribute)) {
                    var c = l.geometry.attributes.position.values.length / 3, g = new Uint8Array(c);
                    g = e._offsetAttribute === C.GeometryOffsetAttribute.TOP ? C.arrayFill(g, 1, 0, c / 2) : (s = e._offsetAttribute === C.GeometryOffsetAttribute.NONE ? 0 : 1, C.arrayFill(g, s)), l.geometry.attributes.applyOffset = new G.GeometryAttribute({
                        componentDatatype: _.ComponentDatatype.UNSIGNED_BYTE,
                        componentsPerAttribute: 1,
                        values: g
                    })
                }
                u.push(l)
            } else for (y = 0; y < a.length; y++) {
                if ((l = R(t, a[y], p, o, n)).geometry.attributes.position.values = I.PolygonPipeline.scaleToGeodeticHeight(l.geometry.attributes.position.values, d, t, !o), v.defined(e._offsetAttribute)) {
                    var h = l.geometry.attributes.position.values.length, m = new Uint8Array(h / 3);
                    s = e._offsetAttribute === C.GeometryOffsetAttribute.NONE ? 0 : 1, C.arrayFill(m, s), l.geometry.attributes.applyOffset = new G.GeometryAttribute({
                        componentDatatype: _.ComponentDatatype.UNSIGNED_BYTE,
                        componentsPerAttribute: 1,
                        values: m
                    })
                }
                u.push(l)
            }
            var b = T.GeometryPipeline.combineInstances(u)[0],
                P = A.BoundingSphere.fromVertices(b.attributes.position.values);
            return new G.Geometry({
                attributes: b.attributes,
                indices: b.indices,
                primitiveType: b.primitiveType,
                boundingSphere: P,
                offsetAttribute: e._offsetAttribute
            })
        }
    }, function (e, t) {
        return v.defined(t) && (e = c.unpack(e, t)), e._ellipsoid = f.Ellipsoid.clone(e._ellipsoid), c.createGeometry(e)
    }
});
