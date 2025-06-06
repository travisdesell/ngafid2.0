define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./AttributeCompression-6cb5b251", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./WebMercatorProjection-44bf888f", "./createTaskProcessorWorker", "./EllipsoidTangentPlane-30395e74", "./OrientedBoundingBox-e6450288", "./TerrainEncoding-32e7a288"], function (We, e, Fe, Oe, Ye, ke, t, i, n, r, a, Ue, o, Ve, He, Le) {
    "use strict";
    var De = Uint16Array.BYTES_PER_ELEMENT, Ge = Int32Array.BYTES_PER_ELEMENT, je = Uint32Array.BYTES_PER_ELEMENT,
        ze = Float32Array.BYTES_PER_ELEMENT, qe = Float64Array.BYTES_PER_ELEMENT;

    function Je(e, t, i) {
        i = We.defaultValue(i, Fe.CesiumMath);
        for (var n = e.length, r = 0; r < n; ++r) if (i.equalsEpsilon(e[r], t, Fe.CesiumMath.EPSILON12)) return r;
        return -1
    }

    var Ke = new Oe.Cartographic, Qe = new Oe.Cartesian3, Xe = new Oe.Cartesian3, Ze = new Oe.Cartesian3,
        $e = new Ye.Matrix4;

    function et(e, t, i, n, r, a, o, s, u, h) {
        for (var d = o.length, c = 0; c < d; ++c) {
            var g = o[c], l = g.cartographic, m = g.index, p = e.length, v = l.longitude, I = l.latitude;
            I = Fe.CesiumMath.clamp(I, -Fe.CesiumMath.PI_OVER_TWO, Fe.CesiumMath.PI_OVER_TWO);
            var f = l.height - a.skirtHeight;
            a.hMin = Math.min(a.hMin, f), Oe.Cartographic.fromRadians(v, I, f, Ke), u && (Ke.longitude += s), u ? c === d - 1 ? Ke.latitude += h : 0 === c && (Ke.latitude -= h) : Ke.latitude += s;
            var E = a.ellipsoid.cartographicToCartesian(Ke);
            e.push(E), t.push(f), i.push(Oe.Cartesian2.clone(i[m])), 0 < n.length && n.push(n[m]), Ye.Matrix4.multiplyByPoint(a.toENU, E, Qe);
            var T = a.minimum, C = a.maximum;
            Oe.Cartesian3.minimumByComponent(Qe, T, T), Oe.Cartesian3.maximumByComponent(Qe, C, C);
            var M = a.lastBorderPoint;
            if (We.defined(M)) {
                var N = M.index;
                r.push(N, p - 1, p, p, m, N)
            }
            a.lastBorderPoint = g
        }
    }

    return o(function (e, t) {
        e.ellipsoid = Oe.Ellipsoid.clone(e.ellipsoid), e.rectangle = Oe.Rectangle.clone(e.rectangle);
        var i = function (e, t, i, n, r, a, o, s, u, h) {
                var d, c, g, l, m, p;
                p = We.defined(n) ? (d = n.west, c = n.south, g = n.east, l = n.north, m = n.width, n.height) : (d = Fe.CesiumMath.toRadians(r.west), c = Fe.CesiumMath.toRadians(r.south), g = Fe.CesiumMath.toRadians(r.east), l = Fe.CesiumMath.toRadians(r.north), m = Fe.CesiumMath.toRadians(n.width), Fe.CesiumMath.toRadians(n.height));
                var v, I, f = [c, l], E = [d, g], T = Ye.Transforms.eastNorthUpToFixedFrame(t, i),
                    C = Ye.Matrix4.inverseTransformation(T, $e);
                s && (v = Ue.WebMercatorProjection.geodeticLatitudeToMercatorAngle(c), I = 1 / (Ue.WebMercatorProjection.geodeticLatitudeToMercatorAngle(l) - v));
                var M = new DataView(e), N = Number.POSITIVE_INFINITY, x = Number.NEGATIVE_INFINITY, b = Xe;
                b.x = Number.POSITIVE_INFINITY, b.y = Number.POSITIVE_INFINITY, b.z = Number.POSITIVE_INFINITY;
                var S = Ze;
                S.x = Number.NEGATIVE_INFINITY, S.y = Number.NEGATIVE_INFINITY, S.z = Number.NEGATIVE_INFINITY;
                var w, P, B = 0, y = 0, A = 0;
                for (P = 0; P < 4; ++P) {
                    var R = B;
                    w = M.getUint32(R, !0), R += je;
                    var _ = Fe.CesiumMath.toRadians(180 * M.getFloat64(R, !0));
                    R += qe, -1 === Je(E, _) && E.push(_);
                    var W = Fe.CesiumMath.toRadians(180 * M.getFloat64(R, !0));
                    R += qe, -1 === Je(f, W) && f.push(W), R += 2 * qe;
                    var F = M.getInt32(R, !0);
                    R += Ge, y += F, F = M.getInt32(R, !0), A += 3 * F, B += w + je
                }
                var O = [], Y = [], k = new Array(y), U = new Array(y), V = new Array(y), H = s ? new Array(y) : [],
                    L = new Array(A), D = [], G = [], j = [], z = [], q = 0, J = 0;
                for (P = B = 0; P < 4; ++P) {
                    w = M.getUint32(B, !0);
                    var K = B += je, Q = Fe.CesiumMath.toRadians(180 * M.getFloat64(B, !0));
                    B += qe;
                    var X = Fe.CesiumMath.toRadians(180 * M.getFloat64(B, !0));
                    B += qe;
                    var Z = Fe.CesiumMath.toRadians(180 * M.getFloat64(B, !0)), $ = .5 * Z;
                    B += qe;
                    var ee = Fe.CesiumMath.toRadians(180 * M.getFloat64(B, !0)), te = .5 * ee;
                    B += qe;
                    var ie = M.getInt32(B, !0);
                    B += Ge;
                    var ne = M.getInt32(B, !0);
                    B += Ge, B += Ge;
                    for (var re = new Array(ie), ae = 0; ae < ie; ++ae) {
                        var oe = Q + M.getUint8(B++) * Z;
                        Ke.longitude = oe;
                        var se = X + M.getUint8(B++) * ee;
                        Ke.latitude = se;
                        var ue = M.getFloat32(B, !0);
                        if (B += ze, 0 !== ue && ue < h && (ue *= -Math.pow(2, u)), ue *= 6371010 * a, Ke.height = ue, -1 !== Je(E, oe) || -1 !== Je(f, se)) {
                            var he = Je(O, Ke, Oe.Cartographic);
                            if (-1 !== he) {
                                re[ae] = Y[he];
                                continue
                            }
                            O.push(Oe.Cartographic.clone(Ke)), Y.push(q)
                        }
                        re[ae] = q, Math.abs(oe - d) < $ ? D.push({
                            index: q,
                            cartographic: Oe.Cartographic.clone(Ke)
                        }) : Math.abs(oe - g) < $ ? j.push({
                            index: q,
                            cartographic: Oe.Cartographic.clone(Ke)
                        }) : Math.abs(se - c) < te ? G.push({
                            index: q,
                            cartographic: Oe.Cartographic.clone(Ke)
                        }) : Math.abs(se - l) < te && z.push({
                            index: q,
                            cartographic: Oe.Cartographic.clone(Ke)
                        }), N = Math.min(ue, N), x = Math.max(ue, x), V[q] = ue;
                        var de = i.cartographicToCartesian(Ke);
                        k[q] = de, s && (H[q] = (Ue.WebMercatorProjection.geodeticLatitudeToMercatorAngle(se) - v) * I), Ye.Matrix4.multiplyByPoint(C, de, Qe), Oe.Cartesian3.minimumByComponent(Qe, b, b), Oe.Cartesian3.maximumByComponent(Qe, S, S);
                        var ce = (oe - d) / (g - d);
                        ce = Fe.CesiumMath.clamp(ce, 0, 1);
                        var ge = (se - c) / (l - c);
                        ge = Fe.CesiumMath.clamp(ge, 0, 1), U[q] = new Oe.Cartesian2(ce, ge), ++q
                    }
                    for (var le = 3 * ne, me = 0; me < le; ++me, ++J) L[J] = re[M.getUint16(B, !0)], B += De;
                    if (w !== B - K) throw new ke.RuntimeError("Invalid terrain tile.")
                }
                k.length = q, U.length = q, V.length = q, s && (H.length = q);
                var pe = q, ve = J,
                    Ie = {hMin: N, lastBorderPoint: void 0, skirtHeight: o, toENU: C, ellipsoid: i, minimum: b, maximum: S};
                D.sort(function (e, t) {
                    return t.cartographic.latitude - e.cartographic.latitude
                }), G.sort(function (e, t) {
                    return e.cartographic.longitude - t.cartographic.longitude
                }), j.sort(function (e, t) {
                    return e.cartographic.latitude - t.cartographic.latitude
                }), z.sort(function (e, t) {
                    return t.cartographic.longitude - e.cartographic.longitude
                });
                var fe = 1e-5;
                if (et(k, V, U, H, L, Ie, D, -fe * m, !0, -fe * p), et(k, V, U, H, L, Ie, G, -fe * p, !1), et(k, V, U, H, L, Ie, j, fe * m, !0, fe * p), et(k, V, U, H, L, Ie, z, fe * p, !1), 0 < D.length && 0 < z.length) {
                    var Ee = D[0].index, Te = z[z.length - 1].index, Ce = k.length - 1;
                    L.push(Te, Ce, pe, pe, Ee, Te)
                }
                y = k.length;
                var Me, Ne = Ye.BoundingSphere.fromPoints(k);
                We.defined(n) && (Me = He.OrientedBoundingBox.fromRectangle(n, N, x, i));
                for (var xe = new Le.EllipsoidalOccluder(i).computeHorizonCullingPointPossiblyUnderEllipsoid(t, k, N), be = new Ve.AxisAlignedBoundingBox(b, S, t), Se = new Le.TerrainEncoding(be, Ie.hMin, x, T, !1, s), we = new Float32Array(y * Se.getStride()), Pe = 0, Be = 0; Be < y; ++Be) Pe = Se.encode(we, Pe, k[Be], U[Be], V[Be], void 0, H[Be]);
                var ye = D.map(function (e) {
                    return e.index
                }).reverse(), Ae = G.map(function (e) {
                    return e.index
                }).reverse(), Re = j.map(function (e) {
                    return e.index
                }).reverse(), _e = z.map(function (e) {
                    return e.index
                }).reverse();
                return Ae.unshift(Re[Re.length - 1]), Ae.push(ye[0]), _e.unshift(ye[ye.length - 1]), _e.push(Re[0]), {
                    vertices: we,
                    indices: new Uint16Array(L),
                    maximumHeight: x,
                    minimumHeight: N,
                    encoding: Se,
                    boundingSphere3D: Ne,
                    orientedBoundingBox: Me,
                    occludeePointInScaledSpace: xe,
                    vertexCountWithoutSkirts: pe,
                    indexCountWithoutSkirts: ve,
                    westIndicesSouthToNorth: ye,
                    southIndicesEastToWest: Ae,
                    eastIndicesNorthToSouth: Re,
                    northIndicesWestToEast: _e
                }
            }(e.buffer, e.relativeToCenter, e.ellipsoid, e.rectangle, e.nativeRectangle, e.exaggeration, e.skirtHeight, e.includeWebMercatorT, e.negativeAltitudeExponentBias, e.negativeElevationThreshold),
            n = i.vertices;
        t.push(n.buffer);
        var r = i.indices;
        return t.push(r.buffer), {
            vertices: n.buffer,
            indices: r.buffer,
            numberOfAttributes: i.encoding.getStride(),
            minimumHeight: i.minimumHeight,
            maximumHeight: i.maximumHeight,
            boundingSphere3D: i.boundingSphere3D,
            orientedBoundingBox: i.orientedBoundingBox,
            occludeePointInScaledSpace: i.occludeePointInScaledSpace,
            encoding: i.encoding,
            vertexCountWithoutSkirts: i.vertexCountWithoutSkirts,
            indexCountWithoutSkirts: i.indexCountWithoutSkirts,
            westIndicesSouthToNorth: i.westIndicesSouthToNorth,
            southIndicesEastToWest: i.southIndicesEastToWest,
            eastIndicesNorthToSouth: i.eastIndicesNorthToSouth,
            northIndicesWestToEast: i.northIndicesWestToEast
        }
    })
});
