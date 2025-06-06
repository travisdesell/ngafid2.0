define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./GeometryInstance-b43ca1c5", "./arrayRemoveDuplicates-a580a060", "./BoundingRectangle-8481a283", "./EllipsoidTangentPlane-30395e74", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f", "./PolygonGeometryLibrary-0de16e21", "./EllipsoidGeodesic-19bdf744"], function (Y, e, U, j, Q, t, r, q, K, a, o, D, i, L, n, s, Z, v, N, l, u, H, c, p, R, M, y) {
    "use strict";
    var m = new j.Cartographic, g = new j.Cartographic;

    function J(e, t, r, a) {
        var o = a.cartesianToCartographic(e, m).height, i = a.cartesianToCartographic(t, g);
        i.height = o, a.cartographicToCartesian(i, t);
        var n = a.cartesianToCartographic(r, g);
        n.height = o - 100, a.cartographicToCartesian(n, r)
    }

    var S = new u.BoundingRectangle, X = new j.Cartesian3, $ = new j.Cartesian3, ee = new j.Cartesian3,
        te = new j.Cartesian3, re = new j.Cartesian3, ae = new j.Cartesian3, oe = new j.Cartesian3,
        ie = new j.Cartesian3, ne = new j.Cartesian3, se = new j.Cartesian2, le = new j.Cartesian2,
        ue = new j.Cartesian3, pe = new Q.Quaternion, ce = new Q.Matrix3, ye = new Q.Matrix3;

    function B(e) {
        var t = e.vertexFormat, r = e.geometry, a = e.shadowVolume, o = r.attributes.position.values, i = o.length,
            n = e.wall, s = e.top || n, l = e.bottom || n;
        if (t.st || t.normal || t.tangent || t.bitangent || a) {
            var u = e.boundingRectangle, p = e.tangentPlane, c = e.ellipsoid, y = e.stRotation, m = e.perPositionHeight,
                g = se;
            g.x = u.x, g.y = u.y;
            var d, h = t.st ? new Float32Array(i / 3 * 2) : void 0;
            t.normal && (d = m && s && !n ? r.attributes.normal.values : new Float32Array(i));
            var f = t.tangent ? new Float32Array(i) : void 0, v = t.bitangent ? new Float32Array(i) : void 0,
                b = a ? new Float32Array(i) : void 0, _ = 0, P = 0, C = $, w = ee, x = te, T = !0, I = ce, A = ye;
            if (0 !== y) {
                var E = Q.Quaternion.fromAxisAngle(p._plane.normal, y, pe);
                I = Q.Matrix3.fromQuaternion(E, I), E = Q.Quaternion.fromAxisAngle(p._plane.normal, -y, pe), A = Q.Matrix3.fromQuaternion(E, A)
            } else I = Q.Matrix3.clone(Q.Matrix3.IDENTITY, I), A = Q.Matrix3.clone(Q.Matrix3.IDENTITY, A);
            var G = 0, O = 0;
            s && l && (G = i / 2, O = i / 3, i /= 2);
            for (var V = 0; V < i; V += 3) {
                var F = j.Cartesian3.fromArray(o, V, ue);
                if (t.st) {
                    var D = Q.Matrix3.multiplyByVector(I, F, X);
                    D = c.scaleToGeodeticSurface(D, D);
                    var L = p.projectPointOntoPlane(D, le);
                    j.Cartesian2.subtract(L, g, L);
                    var N = U.CesiumMath.clamp(L.x / u.width, 0, 1), H = U.CesiumMath.clamp(L.y / u.height, 0, 1);
                    l && (h[_ + O] = N, h[_ + 1 + O] = H), s && (h[_] = N, h[_ + 1] = H), _ += 2
                }
                if (t.normal || t.tangent || t.bitangent || a) {
                    var R = P + 1, M = P + 2;
                    if (n) {
                        if (V + 3 < i) {
                            var S = j.Cartesian3.fromArray(o, V + 3, re);
                            if (T) {
                                var B = j.Cartesian3.fromArray(o, V + i, ae);
                                m && J(F, S, B, c), j.Cartesian3.subtract(S, F, S), j.Cartesian3.subtract(B, F, B), C = j.Cartesian3.normalize(j.Cartesian3.cross(B, S, C), C), T = !1
                            }
                            j.Cartesian3.equalsEpsilon(S, F, U.CesiumMath.EPSILON10) && (T = !0)
                        }
                        (t.tangent || t.bitangent) && (x = c.geodeticSurfaceNormal(F, x), t.tangent && (w = j.Cartesian3.normalize(j.Cartesian3.cross(x, C, w), w)))
                    } else C = c.geodeticSurfaceNormal(F, C), (t.tangent || t.bitangent) && (m && (oe = j.Cartesian3.fromArray(d, P, oe), ie = j.Cartesian3.cross(j.Cartesian3.UNIT_Z, oe, ie), ie = j.Cartesian3.normalize(Q.Matrix3.multiplyByVector(A, ie, ie), ie), t.bitangent && (ne = j.Cartesian3.normalize(j.Cartesian3.cross(oe, ie, ne), ne))), w = j.Cartesian3.cross(j.Cartesian3.UNIT_Z, C, w), w = j.Cartesian3.normalize(Q.Matrix3.multiplyByVector(A, w, w), w), t.bitangent && (x = j.Cartesian3.normalize(j.Cartesian3.cross(C, w, x), x)));
                    t.normal && (e.wall ? (d[P + G] = C.x, d[R + G] = C.y, d[M + G] = C.z) : l && (d[P + G] = -C.x, d[R + G] = -C.y, d[M + G] = -C.z), (s && !m || n) && (d[P] = C.x, d[R] = C.y, d[M] = C.z)), a && (n && (C = c.geodeticSurfaceNormal(F, C)), b[P + G] = -C.x, b[R + G] = -C.y, b[M + G] = -C.z), t.tangent && (e.wall ? (f[P + G] = w.x, f[R + G] = w.y, f[M + G] = w.z) : l && (f[P + G] = -w.x, f[R + G] = -w.y, f[M + G] = -w.z), s && (m ? (f[P] = ie.x, f[R] = ie.y, f[M] = ie.z) : (f[P] = w.x, f[R] = w.y, f[M] = w.z))), t.bitangent && (l && (v[P + G] = x.x, v[R + G] = x.y, v[M + G] = x.z), s && (m ? (v[P] = ne.x, v[R] = ne.y, v[M] = ne.z) : (v[P] = x.x, v[R] = x.y, v[M] = x.z))), P += 3
                }
            }
            t.st && (r.attributes.st = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: h
            })), t.normal && (r.attributes.normal = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: d
            })), t.tangent && (r.attributes.tangent = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: f
            })), t.bitangent && (r.attributes.bitangent = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: v
            })), a && (r.attributes.extrudeDirection = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.FLOAT,
                componentsPerAttribute: 3,
                values: b
            }))
        }
        if (e.extrude && Y.defined(e.offsetAttribute)) {
            var k = o.length / 3, z = new Uint8Array(k);
            if (e.offsetAttribute === Z.GeometryOffsetAttribute.TOP) s && l || n ? z = Z.arrayFill(z, 1, 0, k / 2) : s && (z = Z.arrayFill(z, 1)); else {
                var W = e.offsetAttribute === Z.GeometryOffsetAttribute.NONE ? 0 : 1;
                z = Z.arrayFill(z, W)
            }
            r.attributes.applyOffset = new K.GeometryAttribute({
                componentDatatype: q.ComponentDatatype.UNSIGNED_BYTE,
                componentsPerAttribute: 1,
                values: z
            })
        }
        return r
    }

    var d = new j.Cartographic, h = new j.Cartographic, f = {westOverIDL: 0, eastOverIDL: 0},
        b = new y.EllipsoidGeodesic;

    function _(e, t, r, a, o) {
        if (o = Y.defaultValue(o, new j.Rectangle), !Y.defined(e) || e.length < 3) return o.west = 0, o.north = 0, o.south = 0, o.east = 0, o;
        if (r === c.ArcType.RHUMB) return j.Rectangle.fromCartesianArray(e, t, o);
        b.ellipsoid.equals(t) || (b = new y.EllipsoidGeodesic(void 0, void 0, t)), o.west = Number.POSITIVE_INFINITY, o.east = Number.NEGATIVE_INFINITY, o.south = Number.POSITIVE_INFINITY, o.north = Number.NEGATIVE_INFINITY, f.westOverIDL = Number.POSITIVE_INFINITY, f.eastOverIDL = Number.NEGATIVE_INFINITY;
        for (var i, n = 1 / U.CesiumMath.chordLength(a, t.maximumRadius), s = e.length, l = t.cartesianToCartographic(e[0], h), u = d, p = 1; p < s; p++) i = u, u = l, l = t.cartesianToCartographic(e[p], i), b.setEndPoints(u, l), C(b, n, o, f);
        return i = u, u = l, l = t.cartesianToCartographic(e[0], i), b.setEndPoints(u, l), C(b, n, o, f), o.east - o.west > f.eastOverIDL - f.westOverIDL && (o.west = f.westOverIDL, o.east = f.eastOverIDL, o.east > U.CesiumMath.PI && (o.east = o.east - U.CesiumMath.TWO_PI), o.west > U.CesiumMath.PI && (o.west = o.west - U.CesiumMath.TWO_PI)), o
    }

    var P = new j.Cartographic;

    function C(e, t, r, a) {
        for (var o = e.surfaceDistance, i = Math.ceil(o * t), n = 0 < i ? o / (i - 1) : Number.POSITIVE_INFINITY, s = 0, l = 0; l < i; l++) {
            var u = e.interpolateUsingSurfaceDistance(s, P);
            s += n;
            var p = u.longitude, c = u.latitude;
            r.west = Math.min(r.west, p), r.east = Math.max(r.east, p), r.south = Math.min(r.south, c), r.north = Math.max(r.north, c);
            var y = 0 <= p ? p : p + U.CesiumMath.TWO_PI;
            a.westOverIDL = Math.min(a.westOverIDL, y), a.eastOverIDL = Math.max(a.eastOverIDL, y)
        }
    }

    var O = [];

    function k(e, t, r, a, o, i, n, s, l) {
        var u, p = {walls: []};
        if (i || n) {
            var c, y, m = M.PolygonGeometryLibrary.createGeometryFromPositions(e, t, r, o, s, l),
                g = m.attributes.position.values, d = m.indices;
            if (i && n) {
                var h = g.concat(g);
                c = h.length / 3, (y = L.IndexDatatype.createTypedArray(c, 2 * d.length)).set(d);
                var f = d.length, v = c / 2;
                for (u = 0; u < f; u += 3) {
                    var b = y[u] + v, _ = y[u + 1] + v, P = y[u + 2] + v;
                    y[u + f] = P, y[u + 1 + f] = _, y[u + 2 + f] = b
                }
                if (m.attributes.position.values = h, o && s.normal) {
                    var C = m.attributes.normal.values;
                    m.attributes.normal.values = new Float32Array(h.length), m.attributes.normal.values.set(C)
                }
                m.indices = y
            } else if (n) {
                for (c = g.length / 3, y = L.IndexDatatype.createTypedArray(c, d.length), u = 0; u < d.length; u += 3) y[u] = d[u + 2], y[u + 1] = d[u + 1], y[u + 2] = d[u];
                m.indices = y
            }
            p.topAndBottom = new N.GeometryInstance({geometry: m})
        }
        var w = a.outerRing, x = H.EllipsoidTangentPlane.fromPoints(w, e), T = x.projectPointsOntoPlane(w, O),
            I = R.PolygonPipeline.computeWindingOrder2D(T);
        I === R.WindingOrder.CLOCKWISE && (w = w.slice().reverse());
        var A = M.PolygonGeometryLibrary.computeWallGeometry(w, e, r, o, l);
        p.walls.push(new N.GeometryInstance({geometry: A}));
        var E = a.holes;
        for (u = 0; u < E.length; u++) {
            var G = E[u];
            T = (x = H.EllipsoidTangentPlane.fromPoints(G, e)).projectPointsOntoPlane(G, O), (I = R.PolygonPipeline.computeWindingOrder2D(T)) === R.WindingOrder.COUNTER_CLOCKWISE && (G = G.slice().reverse()), A = M.PolygonGeometryLibrary.computeWallGeometry(G, e, r, o, l), p.walls.push(new N.GeometryInstance({geometry: A}))
        }
        return p
    }

    function w(e) {
        var t = e.polygonHierarchy, r = Y.defaultValue(e.vertexFormat, v.VertexFormat.DEFAULT),
            a = Y.defaultValue(e.ellipsoid, j.Ellipsoid.WGS84),
            o = Y.defaultValue(e.granularity, U.CesiumMath.RADIANS_PER_DEGREE), i = Y.defaultValue(e.stRotation, 0),
            n = Y.defaultValue(e.perPositionHeight, !1), s = n && Y.defined(e.extrudedHeight),
            l = Y.defaultValue(e.height, 0), u = Y.defaultValue(e.extrudedHeight, l);
        if (!s) {
            var p = Math.max(l, u);
            u = Math.min(l, u), l = p
        }
        this._vertexFormat = v.VertexFormat.clone(r), this._ellipsoid = j.Ellipsoid.clone(a), this._granularity = o, this._stRotation = i, this._height = l, this._extrudedHeight = u, this._closeTop = Y.defaultValue(e.closeTop, !0), this._closeBottom = Y.defaultValue(e.closeBottom, !0), this._polygonHierarchy = t, this._perPositionHeight = n, this._perPositionHeightExtrude = s, this._shadowVolume = Y.defaultValue(e.shadowVolume, !1), this._workerName = "createPolygonGeometry", this._offsetAttribute = e.offsetAttribute, this._arcType = Y.defaultValue(e.arcType, c.ArcType.GEODESIC), this._rectangle = void 0, this._textureCoordinateRotationPoints = void 0, this.packedLength = M.PolygonGeometryLibrary.computeHierarchyPackedLength(t) + j.Ellipsoid.packedLength + v.VertexFormat.packedLength + 12
    }

    w.fromPositions = function (e) {
        return new w({
            polygonHierarchy: {positions: (e = Y.defaultValue(e, Y.defaultValue.EMPTY_OBJECT)).positions},
            height: e.height,
            extrudedHeight: e.extrudedHeight,
            vertexFormat: e.vertexFormat,
            stRotation: e.stRotation,
            ellipsoid: e.ellipsoid,
            granularity: e.granularity,
            perPositionHeight: e.perPositionHeight,
            closeTop: e.closeTop,
            closeBottom: e.closeBottom,
            offsetAttribute: e.offsetAttribute,
            arcType: e.arcType
        })
    }, w.pack = function (e, t, r) {
        return r = Y.defaultValue(r, 0), r = M.PolygonGeometryLibrary.packPolygonHierarchy(e._polygonHierarchy, t, r), j.Ellipsoid.pack(e._ellipsoid, t, r), r += j.Ellipsoid.packedLength, v.VertexFormat.pack(e._vertexFormat, t, r), r += v.VertexFormat.packedLength, t[r++] = e._height, t[r++] = e._extrudedHeight, t[r++] = e._granularity, t[r++] = e._stRotation, t[r++] = e._perPositionHeightExtrude ? 1 : 0, t[r++] = e._perPositionHeight ? 1 : 0, t[r++] = e._closeTop ? 1 : 0, t[r++] = e._closeBottom ? 1 : 0, t[r++] = e._shadowVolume ? 1 : 0, t[r++] = Y.defaultValue(e._offsetAttribute, -1), t[r++] = e._arcType, t[r] = e.packedLength, t
    };
    var x = j.Ellipsoid.clone(j.Ellipsoid.UNIT_SPHERE), T = new v.VertexFormat, I = {polygonHierarchy: {}};
    return w.unpack = function (e, t, r) {
        t = Y.defaultValue(t, 0);
        var a = M.PolygonGeometryLibrary.unpackPolygonHierarchy(e, t);
        t = a.startingIndex, delete a.startingIndex;
        var o = j.Ellipsoid.unpack(e, t, x);
        t += j.Ellipsoid.packedLength;
        var i = v.VertexFormat.unpack(e, t, T);
        t += v.VertexFormat.packedLength;
        var n = e[t++], s = e[t++], l = e[t++], u = e[t++], p = 1 === e[t++], c = 1 === e[t++], y = 1 === e[t++],
            m = 1 === e[t++], g = 1 === e[t++], d = e[t++], h = e[t++], f = e[t];
        return Y.defined(r) || (r = new w(I)), r._polygonHierarchy = a, r._ellipsoid = j.Ellipsoid.clone(o, r._ellipsoid), r._vertexFormat = v.VertexFormat.clone(i, r._vertexFormat), r._height = n, r._extrudedHeight = s, r._granularity = l, r._stRotation = u, r._perPositionHeightExtrude = p, r._perPositionHeight = c, r._closeTop = y, r._closeBottom = m, r._shadowVolume = g, r._offsetAttribute = -1 === d ? void 0 : d, r._arcType = h, r.packedLength = f, r
    }, w.computeRectangle = function (e, t) {
        var r = Y.defaultValue(e.granularity, U.CesiumMath.RADIANS_PER_DEGREE),
            a = Y.defaultValue(e.arcType, c.ArcType.GEODESIC), o = e.polygonHierarchy,
            i = Y.defaultValue(e.ellipsoid, j.Ellipsoid.WGS84);
        return _(o.positions, i, a, r, t)
    }, w.createGeometry = function (e) {
        var t = e._vertexFormat, r = e._ellipsoid, a = e._granularity, o = e._stRotation, i = e._polygonHierarchy,
            n = e._perPositionHeight, s = e._closeTop, l = e._closeBottom, u = e._arcType, p = i.positions;
        if (!(p.length < 3)) {
            var c = H.EllipsoidTangentPlane.fromPoints(p, r),
                y = M.PolygonGeometryLibrary.polygonsFromHierarchy(i, c.projectPointsOntoPlane.bind(c), !n, r),
                m = y.hierarchy, g = y.polygons;
            if (0 !== m.length) {
                p = m[0].outerRing;
                var d,
                    h = M.PolygonGeometryLibrary.computeBoundingRectangle(c.plane.normal, c.projectPointOntoPlane.bind(c), p, o, S),
                    f = [], v = e._height, b = e._extrudedHeight, _ = {
                        perPositionHeight: n,
                        vertexFormat: t,
                        geometry: void 0,
                        tangentPlane: c,
                        boundingRectangle: h,
                        ellipsoid: r,
                        stRotation: o,
                        bottom: !1,
                        top: !0,
                        wall: !1,
                        extrude: !1,
                        arcType: u
                    };
                if (e._perPositionHeightExtrude || !U.CesiumMath.equalsEpsilon(v, b, 0, U.CesiumMath.EPSILON2)) for (_.extrude = !0, _.top = s, _.bottom = l, _.shadowVolume = e._shadowVolume, _.offsetAttribute = e._offsetAttribute, d = 0; d < g.length; d++) {
                    var P, C = k(r, g[d], a, m[d], n, s, l, t, u);
                    s && l ? (P = C.topAndBottom, _.geometry = M.PolygonGeometryLibrary.scaleToGeodeticHeightExtruded(P.geometry, v, b, r, n)) : s ? ((P = C.topAndBottom).geometry.attributes.position.values = R.PolygonPipeline.scaleToGeodeticHeight(P.geometry.attributes.position.values, v, r, !n), _.geometry = P.geometry) : l && ((P = C.topAndBottom).geometry.attributes.position.values = R.PolygonPipeline.scaleToGeodeticHeight(P.geometry.attributes.position.values, b, r, !0), _.geometry = P.geometry), (s || l) && (_.wall = !1, P.geometry = B(_), f.push(P));
                    var w = C.walls;
                    _.wall = !0;
                    for (var x = 0; x < w.length; x++) {
                        var T = w[x];
                        _.geometry = M.PolygonGeometryLibrary.scaleToGeodeticHeightExtruded(T.geometry, v, b, r, n), T.geometry = B(_), f.push(T)
                    }
                } else for (d = 0; d < g.length; d++) {
                    var I = new N.GeometryInstance({geometry: M.PolygonGeometryLibrary.createGeometryFromPositions(r, g[d], a, n, t, u)});
                    if (I.geometry.attributes.position.values = R.PolygonPipeline.scaleToGeodeticHeight(I.geometry.attributes.position.values, v, r, !n), _.geometry = I.geometry, I.geometry = B(_), Y.defined(e._offsetAttribute)) {
                        var A = I.geometry.attributes.position.values.length, E = new Uint8Array(A / 3),
                            G = e._offsetAttribute === Z.GeometryOffsetAttribute.NONE ? 0 : 1;
                        Z.arrayFill(E, G), I.geometry.attributes.applyOffset = new K.GeometryAttribute({
                            componentDatatype: q.ComponentDatatype.UNSIGNED_BYTE,
                            componentsPerAttribute: 1,
                            values: E
                        })
                    }
                    f.push(I)
                }
                var O = D.GeometryPipeline.combineInstances(f)[0];
                O.attributes.position.values = new Float64Array(O.attributes.position.values), O.indices = L.IndexDatatype.createTypedArray(O.attributes.position.values.length / 3, O.indices);
                var V = O.attributes, F = Q.BoundingSphere.fromVertices(V.position.values);
                return t.position || delete V.position, new K.Geometry({
                    attributes: V,
                    indices: O.indices,
                    primitiveType: O.primitiveType,
                    boundingSphere: F,
                    offsetAttribute: e._offsetAttribute
                })
            }
        }
    }, w.createShadowVolume = function (e, t, r) {
        var a = e._granularity, o = e._ellipsoid, i = t(a, o), n = r(a, o);
        return new w({
            polygonHierarchy: e._polygonHierarchy,
            ellipsoid: o,
            stRotation: e._stRotation,
            granularity: a,
            perPositionHeight: !1,
            extrudedHeight: i,
            height: n,
            vertexFormat: v.VertexFormat.POSITION_ONLY,
            shadowVolume: !0,
            arcType: e._arcType
        })
    }, Object.defineProperties(w.prototype, {
        rectangle: {
            get: function () {
                if (!Y.defined(this._rectangle)) {
                    var e = this._polygonHierarchy.positions;
                    this._rectangle = _(e, this._ellipsoid, this._arcType, this._granularity)
                }
                return this._rectangle
            }
        }, textureCoordinateRotationPoints: {
            get: function () {
                return Y.defined(this._textureCoordinateRotationPoints) || (this._textureCoordinateRotationPoints = function (e) {
                    var t = -e._stRotation;
                    if (0 == t) return [0, 0, 0, 1, 1, 0];
                    var r = e._ellipsoid, a = e._polygonHierarchy.positions, o = e.rectangle;
                    return K.Geometry._textureCoordinateRotationPoints(a, t, r, o)
                }(this)), this._textureCoordinateRotationPoints
            }
        }
    }), function (e, t) {
        return Y.defined(t) && (e = w.unpack(e, t)), e._ellipsoid = j.Ellipsoid.clone(e._ellipsoid), w.createGeometry(e)
    }
});
