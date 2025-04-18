define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./RuntimeError-6122571f", "./createTaskProcessorWorker"], function (i, e, S, t) {
    "use strict";
    var h = 1953029805, c = 2917034100;

    function b(e, t) {
        if (b.passThroughDataForTesting) return t;
        var i = e.byteLength;
        if (0 === i || i % 4 != 0) throw new S.RuntimeError("The length of key must be greater than 0 and a multiple of 4.");
        var n = new DataView(t), r = n.getUint32(0, !0);
        if (r === h || r === c) return t;
        for (var a, o = new DataView(e), s = 0, f = t.byteLength, l = f - f % 8, u = i, d = 8; s < l;) for (a = d = (d + 8) % 24; s < l && a < u;) n.setUint32(s, n.getUint32(s, !0) ^ o.getUint32(a, !0), !0), n.setUint32(s + 4, n.getUint32(s + 4, !0) ^ o.getUint32(a + 4, !0), !0), s += 8, a += 24;
        if (s < f) for (u <= a && (a = d = (d + 8) % 24); s < f;) n.setUint8(s, n.getUint8(s) ^ o.getUint8(a)), s++, a++
    }

    function n(e, t) {
        return 0 != (e & t)
    }

    b.passThroughDataForTesting = !1;
    var r = [1, 2, 4, 8];

    function T(e, t, i, n, r, a) {
        this._bits = e, this.cnodeVersion = t, this.imageryVersion = i, this.terrainVersion = n, this.imageryProvider = r, this.terrainProvider = a, this.ancestorHasTerrain = !1, this.terrainState = void 0
    }

    T.clone = function (e, t) {
        return i.defined(t) ? (t._bits = e._bits, t.cnodeVersion = e.cnodeVersion, t.imageryVersion = e.imageryVersion, t.terrainVersion = e.terrainVersion, t.imageryProvider = e.imageryProvider, t.terrainProvider = e.terrainProvider) : t = new T(e._bits, e.cnodeVersion, e.imageryVersion, e.terrainVersion, e.imageryProvider, e.terrainProvider), t.ancestorHasTerrain = e.ancestorHasTerrain, t.terrainState = e.terrainState, t
    }, T.prototype.setParent = function (e) {
        this.ancestorHasTerrain = e.ancestorHasTerrain || this.hasTerrain()
    }, T.prototype.hasSubtree = function () {
        return n(this._bits, 16)
    }, T.prototype.hasImagery = function () {
        return n(this._bits, 64)
    }, T.prototype.hasTerrain = function () {
        return n(this._bits, 128)
    }, T.prototype.hasChildren = function () {
        return n(this._bits, 15)
    }, T.prototype.hasChild = function (e) {
        return n(this._bits, r[e])
    }, T.prototype.getChildBitmask = function () {
        return 15 & this._bits
    };
    var s = function a(o, s, f) {
            function l(i, e) {
                if (!s[i]) {
                    if (!o[i]) {
                        var t = "function" == typeof require && require;
                        if (!e && t) return t(i, !0);
                        if (u) return u(i, !0);
                        var n = new Error("Cannot find module '" + i + "'");
                        throw n.code = "MODULE_NOT_FOUND", n
                    }
                    var r = s[i] = {exports: {}};
                    o[i][0].call(r.exports, function (e) {
                        var t = o[i][1][e];
                        return l(t || e)
                    }, r, r.exports, a, o, s, f)
                }
                return s[i].exports
            }

            for (var u = "function" == typeof require && require, e = 0; e < f.length; e++) l(f[e]);
            return l
        }({
            1: [function (e, t, i) {
                var n = "undefined" != typeof Uint8Array && "undefined" != typeof Uint16Array && "undefined" != typeof Int32Array;
                i.assign = function (e) {
                    for (var t = Array.prototype.slice.call(arguments, 1); t.length;) {
                        var i = t.shift();
                        if (i) {
                            if ("object" != typeof i) throw new TypeError(i + "must be non-object");
                            for (var n in i) i.hasOwnProperty(n) && (e[n] = i[n])
                        }
                    }
                    return e
                }, i.shrinkBuf = function (e, t) {
                    return e.length === t ? e : e.subarray ? e.subarray(0, t) : (e.length = t, e)
                };
                var r = {
                    arraySet: function (e, t, i, n, r) {
                        if (t.subarray && e.subarray) e.set(t.subarray(i, i + n), r); else for (var a = 0; a < n; a++) e[r + a] = t[i + a]
                    }, flattenChunks: function (e) {
                        var t, i, n, r, a, o;
                        for (t = n = 0, i = e.length; t < i; t++) n += e[t].length;
                        for (o = new Uint8Array(n), t = r = 0, i = e.length; t < i; t++) a = e[t], o.set(a, r), r += a.length;
                        return o
                    }
                }, a = {
                    arraySet: function (e, t, i, n, r) {
                        for (var a = 0; a < n; a++) e[r + a] = t[i + a]
                    }, flattenChunks: function (e) {
                        return [].concat.apply([], e)
                    }
                };
                i.setTyped = function (e) {
                    e ? (i.Buf8 = Uint8Array, i.Buf16 = Uint16Array, i.Buf32 = Int32Array, i.assign(i, r)) : (i.Buf8 = Array, i.Buf16 = Array, i.Buf32 = Array, i.assign(i, a))
                }, i.setTyped(n)
            }, {}],
            2: [function (e, t, i) {
                var f = e("./common"), r = !0, a = !0;
                try {
                    String.fromCharCode.apply(null, [0])
                } catch (e) {
                    r = !1
                }
                try {
                    String.fromCharCode.apply(null, new Uint8Array(1))
                } catch (e) {
                    a = !1
                }
                for (var l = new f.Buf8(256), n = 0; n < 256; n++) l[n] = 252 <= n ? 6 : 248 <= n ? 5 : 240 <= n ? 4 : 224 <= n ? 3 : 192 <= n ? 2 : 1;

                function u(e, t) {
                    if (t < 65537 && (e.subarray && a || !e.subarray && r)) return String.fromCharCode.apply(null, f.shrinkBuf(e, t));
                    for (var i = "", n = 0; n < t; n++) i += String.fromCharCode(e[n]);
                    return i
                }

                l[254] = l[254] = 1, i.string2buf = function (e) {
                    var t, i, n, r, a, o = e.length, s = 0;
                    for (r = 0; r < o; r++) 55296 == (64512 & (i = e.charCodeAt(r))) && r + 1 < o && 56320 == (64512 & (n = e.charCodeAt(r + 1))) && (i = 65536 + (i - 55296 << 10) + (n - 56320), r++), s += i < 128 ? 1 : i < 2048 ? 2 : i < 65536 ? 3 : 4;
                    for (t = new f.Buf8(s), r = a = 0; a < s; r++) 55296 == (64512 & (i = e.charCodeAt(r))) && r + 1 < o && 56320 == (64512 & (n = e.charCodeAt(r + 1))) && (i = 65536 + (i - 55296 << 10) + (n - 56320), r++), i < 128 ? t[a++] = i : (i < 2048 ? t[a++] = 192 | i >>> 6 : (i < 65536 ? t[a++] = 224 | i >>> 12 : (t[a++] = 240 | i >>> 18, t[a++] = 128 | i >>> 12 & 63), t[a++] = 128 | i >>> 6 & 63), t[a++] = 128 | 63 & i);
                    return t
                }, i.buf2binstring = function (e) {
                    return u(e, e.length)
                }, i.binstring2buf = function (e) {
                    for (var t = new f.Buf8(e.length), i = 0, n = t.length; i < n; i++) t[i] = e.charCodeAt(i);
                    return t
                }, i.buf2string = function (e, t) {
                    var i, n, r, a, o = t || e.length, s = new Array(2 * o);
                    for (i = n = 0; i < o;) if ((r = e[i++]) < 128) s[n++] = r; else if (4 < (a = l[r])) s[n++] = 65533, i += a - 1; else {
                        for (r &= 2 === a ? 31 : 3 === a ? 15 : 7; 1 < a && i < o;) r = r << 6 | 63 & e[i++], a--;
                        1 < a ? s[n++] = 65533 : r < 65536 ? s[n++] = r : (r -= 65536, s[n++] = 55296 | r >> 10 & 1023, s[n++] = 56320 | 1023 & r)
                    }
                    return u(s, n)
                }, i.utf8border = function (e, t) {
                    var i;
                    for ((t = t || e.length) > e.length && (t = e.length), i = t - 1; 0 <= i && 128 == (192 & e[i]);) i--;
                    return !(i < 0) && 0 !== i && i + l[e[i]] > t ? i : t
                }
            }, {"./common": 1}],
            3: [function (e, t, i) {
                t.exports = function (e, t, i, n) {
                    var r = 65535 & e | 0, a = e >>> 16 & 65535 | 0, o = 0;
                    for (; 0 !== i;) {
                        for (i -= o = 2e3 < i ? 2e3 : i; r = r + t[n++] | 0, a = a + r | 0, --o;) ;
                        r %= 65521, a %= 65521
                    }
                    return r | a << 16 | 0
                }
            }, {}],
            4: [function (e, t, i) {
                t.exports = {
                    Z_NO_FLUSH: 0,
                    Z_PARTIAL_FLUSH: 1,
                    Z_SYNC_FLUSH: 2,
                    Z_FULL_FLUSH: 3,
                    Z_FINISH: 4,
                    Z_BLOCK: 5,
                    Z_TREES: 6,
                    Z_OK: 0,
                    Z_STREAM_END: 1,
                    Z_NEED_DICT: 2,
                    Z_ERRNO: -1,
                    Z_STREAM_ERROR: -2,
                    Z_DATA_ERROR: -3,
                    Z_BUF_ERROR: -5,
                    Z_NO_COMPRESSION: 0,
                    Z_BEST_SPEED: 1,
                    Z_BEST_COMPRESSION: 9,
                    Z_DEFAULT_COMPRESSION: -1,
                    Z_FILTERED: 1,
                    Z_HUFFMAN_ONLY: 2,
                    Z_RLE: 3,
                    Z_FIXED: 4,
                    Z_DEFAULT_STRATEGY: 0,
                    Z_BINARY: 0,
                    Z_TEXT: 1,
                    Z_UNKNOWN: 2,
                    Z_DEFLATED: 8
                }
            }, {}],
            5: [function (e, t, i) {
                var s = function () {
                    for (var e, t = [], i = 0; i < 256; i++) {
                        e = i;
                        for (var n = 0; n < 8; n++) e = 1 & e ? 3988292384 ^ e >>> 1 : e >>> 1;
                        t[i] = e
                    }
                    return t
                }();
                t.exports = function (e, t, i, n) {
                    var r = s, a = n + i;
                    e ^= -1;
                    for (var o = n; o < a; o++) e = e >>> 8 ^ r[255 & (e ^ t[o])];
                    return -1 ^ e
                }
            }, {}],
            6: [function (e, t, i) {
                t.exports = function () {
                    this.text = 0, this.time = 0, this.xflags = 0, this.os = 0, this.extra = null, this.extra_len = 0, this.name = "", this.comment = "", this.hcrc = 0, this.done = !1
                }
            }, {}],
            7: [function (e, t, i) {
                t.exports = function (e, t) {
                    var i, n, r, a, o, s, f, l, u, d, h, c, b, m, w, g, v, k, _, p, y, x, E, S, T;
                    i = e.state, n = e.next_in, S = e.input, r = n + (e.avail_in - 5), a = e.next_out, T = e.output, o = a - (t - e.avail_out), s = a + (e.avail_out - 257), f = i.dmax, l = i.wsize, u = i.whave, d = i.wnext, h = i.window, c = i.hold, b = i.bits, m = i.lencode, w = i.distcode, g = (1 << i.lenbits) - 1, v = (1 << i.distbits) - 1;
                    e:do {
                        b < 15 && (c += S[n++] << b, b += 8, c += S[n++] << b, b += 8), k = m[c & g];
                        t:for (; ;) {
                            if (c >>>= _ = k >>> 24, b -= _, 0 === (_ = k >>> 16 & 255)) T[a++] = 65535 & k; else {
                                if (!(16 & _)) {
                                    if (0 == (64 & _)) {
                                        k = m[(65535 & k) + (c & (1 << _) - 1)];
                                        continue t
                                    }
                                    if (32 & _) {
                                        i.mode = 12;
                                        break e
                                    }
                                    e.msg = "invalid literal/length code", i.mode = 30;
                                    break e
                                }
                                p = 65535 & k, (_ &= 15) && (b < _ && (c += S[n++] << b, b += 8), p += c & (1 << _) - 1, c >>>= _, b -= _), b < 15 && (c += S[n++] << b, b += 8, c += S[n++] << b, b += 8), k = w[c & v];
                                i:for (; ;) {
                                    if (c >>>= _ = k >>> 24, b -= _, !(16 & (_ = k >>> 16 & 255))) {
                                        if (0 == (64 & _)) {
                                            k = w[(65535 & k) + (c & (1 << _) - 1)];
                                            continue i
                                        }
                                        e.msg = "invalid distance code", i.mode = 30;
                                        break e
                                    }
                                    if (y = 65535 & k, b < (_ &= 15) && (c += S[n++] << b, (b += 8) < _ && (c += S[n++] << b, b += 8)), f < (y += c & (1 << _) - 1)) {
                                        e.msg = "invalid distance too far back", i.mode = 30;
                                        break e
                                    }
                                    if (c >>>= _, b -= _, (_ = a - o) < y) {
                                        if (u < (_ = y - _) && i.sane) {
                                            e.msg = "invalid distance too far back", i.mode = 30;
                                            break e
                                        }
                                        if (E = h, (x = 0) === d) {
                                            if (x += l - _, _ < p) {
                                                for (p -= _; T[a++] = h[x++], --_;) ;
                                                x = a - y, E = T
                                            }
                                        } else if (d < _) {
                                            if (x += l + d - _, (_ -= d) < p) {
                                                for (p -= _; T[a++] = h[x++], --_;) ;
                                                if (x = 0, d < p) {
                                                    for (p -= _ = d; T[a++] = h[x++], --_;) ;
                                                    x = a - y, E = T
                                                }
                                            }
                                        } else if (x += d - _, _ < p) {
                                            for (p -= _; T[a++] = h[x++], --_;) ;
                                            x = a - y, E = T
                                        }
                                        for (; 2 < p;) T[a++] = E[x++], T[a++] = E[x++], T[a++] = E[x++], p -= 3;
                                        p && (T[a++] = E[x++], 1 < p && (T[a++] = E[x++]))
                                    } else {
                                        for (x = a - y; T[a++] = T[x++], T[a++] = T[x++], T[a++] = T[x++], 2 < (p -= 3);) ;
                                        p && (T[a++] = T[x++], 1 < p && (T[a++] = T[x++]))
                                    }
                                    break
                                }
                            }
                            break
                        }
                    } while (n < r && a < s);
                    n -= p = b >> 3, c &= (1 << (b -= p << 3)) - 1, e.next_in = n, e.next_out = a, e.avail_in = n < r ? r - n + 5 : 5 - (n - r), e.avail_out = a < s ? s - a + 257 : 257 - (a - s), i.hold = c, i.bits = b
                }
            }, {}],
            8: [function (e, t, i) {
                var B = e("../utils/common"), U = e("./adler32"), Z = e("./crc32"), I = e("./inffast"), D = e("./inftrees"),
                    N = 1, O = 2, z = 0, C = -2, F = 1, n = 852, r = 592;

                function L(e) {
                    return (e >>> 24 & 255) + (e >>> 8 & 65280) + ((65280 & e) << 8) + ((255 & e) << 24)
                }

                function a() {
                    this.mode = 0, this.last = !1, this.wrap = 0, this.havedict = !1, this.flags = 0, this.dmax = 0, this.check = 0, this.total = 0, this.head = null, this.wbits = 0, this.wsize = 0, this.whave = 0, this.wnext = 0, this.window = null, this.hold = 0, this.bits = 0, this.length = 0, this.offset = 0, this.extra = 0, this.lencode = null, this.distcode = null, this.lenbits = 0, this.distbits = 0, this.ncode = 0, this.nlen = 0, this.ndist = 0, this.have = 0, this.next = null, this.lens = new B.Buf16(320), this.work = new B.Buf16(288), this.lendyn = null, this.distdyn = null, this.sane = 0, this.back = 0, this.was = 0
                }

                function o(e) {
                    var t;
                    return e && e.state ? (t = e.state, e.total_in = e.total_out = t.total = 0, e.msg = "", t.wrap && (e.adler = 1 & t.wrap), t.mode = F, t.last = 0, t.havedict = 0, t.dmax = 32768, t.head = null, t.hold = 0, t.bits = 0, t.lencode = t.lendyn = new B.Buf32(n), t.distcode = t.distdyn = new B.Buf32(r), t.sane = 1, t.back = -1, z) : C
                }

                function s(e) {
                    var t;
                    return e && e.state ? ((t = e.state).wsize = 0, t.whave = 0, t.wnext = 0, o(e)) : C
                }

                function f(e, t) {
                    var i, n;
                    return e && e.state ? (n = e.state, t < 0 ? (i = 0, t = -t) : (i = 1 + (t >> 4), t < 48 && (t &= 15)), t && (t < 8 || 15 < t) ? C : (null !== n.window && n.wbits !== t && (n.window = null), n.wrap = i, n.wbits = t, s(e))) : C
                }

                function l(e, t) {
                    var i, n;
                    return e ? (n = new a, (e.state = n).window = null, (i = f(e, t)) !== z && (e.state = null), i) : C
                }

                var u, d, h = !0;

                function H(e) {
                    if (h) {
                        var t;
                        for (u = new B.Buf32(512), d = new B.Buf32(32), t = 0; t < 144;) e.lens[t++] = 8;
                        for (; t < 256;) e.lens[t++] = 9;
                        for (; t < 280;) e.lens[t++] = 7;
                        for (; t < 288;) e.lens[t++] = 8;
                        for (D(N, e.lens, 0, 288, u, 0, e.work, {bits: 9}), t = 0; t < 32;) e.lens[t++] = 5;
                        D(O, e.lens, 0, 32, d, 0, e.work, {bits: 5}), h = !1
                    }
                    e.lencode = u, e.lenbits = 9, e.distcode = d, e.distbits = 5
                }

                function P(e, t, i, n) {
                    var r, a = e.state;
                    return null === a.window && (a.wsize = 1 << a.wbits, a.wnext = 0, a.whave = 0, a.window = new B.Buf8(a.wsize)), n >= a.wsize ? (B.arraySet(a.window, t, i - a.wsize, a.wsize, 0), a.wnext = 0, a.whave = a.wsize) : (r = a.wsize - a.wnext, n < r && (r = n), B.arraySet(a.window, t, i - n, r, a.wnext), (n -= r) ? (B.arraySet(a.window, t, i - n, n, 0), a.wnext = n, a.whave = a.wsize) : (a.wnext += r, a.wnext === a.wsize && (a.wnext = 0), a.whave < a.wsize && (a.whave += r))), 0
                }

                i.inflateReset = s, i.inflateReset2 = f, i.inflateResetKeep = o, i.inflateInit = function (e) {
                    return l(e, 15)
                }, i.inflateInit2 = l, i.inflate = function (e, t) {
                    var i, n, r, a, o, s, f, l, u, d, h, c, b, m, w, g, v, k, _, p, y, x, E, S, T = 0, R = new B.Buf8(4),
                        A = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15];
                    if (!e || !e.state || !e.output || !e.input && 0 !== e.avail_in) return C;
                    12 === (i = e.state).mode && (i.mode = 13);
                    o = e.next_out, r = e.output, f = e.avail_out, a = e.next_in, n = e.input, s = e.avail_in, l = i.hold, u = i.bits, d = s, h = f, x = z;
                    e:for (; ;) switch (i.mode) {
                        case F:
                            if (0 === i.wrap) {
                                i.mode = 13;
                                break
                            }
                            for (; u < 16;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if (2 & i.wrap && 35615 === l) {
                                i.check = 0, R[0] = 255 & l, R[1] = l >>> 8 & 255, i.check = Z(i.check, R, 2, 0), u = l = 0, i.mode = 2;
                                break
                            }
                            if (i.flags = 0, i.head && (i.head.done = !1), !(1 & i.wrap) || (((255 & l) << 8) + (l >> 8)) % 31) {
                                e.msg = "incorrect header check", i.mode = 30;
                                break
                            }
                            if (8 != (15 & l)) {
                                e.msg = "unknown compression method", i.mode = 30;
                                break
                            }
                            if (u -= 4, y = 8 + (15 & (l >>>= 4)), 0 === i.wbits) i.wbits = y; else if (y > i.wbits) {
                                e.msg = "invalid window size", i.mode = 30;
                                break
                            }
                            i.dmax = 1 << y, e.adler = i.check = 1, i.mode = 512 & l ? 10 : 12, u = l = 0;
                            break;
                        case 2:
                            for (; u < 16;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if (i.flags = l, 8 != (255 & i.flags)) {
                                e.msg = "unknown compression method", i.mode = 30;
                                break
                            }
                            if (57344 & i.flags) {
                                e.msg = "unknown header flags set", i.mode = 30;
                                break
                            }
                            i.head && (i.head.text = l >> 8 & 1), 512 & i.flags && (R[0] = 255 & l, R[1] = l >>> 8 & 255, i.check = Z(i.check, R, 2, 0)), u = l = 0, i.mode = 3;
                        case 3:
                            for (; u < 32;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            i.head && (i.head.time = l), 512 & i.flags && (R[0] = 255 & l, R[1] = l >>> 8 & 255, R[2] = l >>> 16 & 255, R[3] = l >>> 24 & 255, i.check = Z(i.check, R, 4, 0)), u = l = 0, i.mode = 4;
                        case 4:
                            for (; u < 16;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            i.head && (i.head.xflags = 255 & l, i.head.os = l >> 8), 512 & i.flags && (R[0] = 255 & l, R[1] = l >>> 8 & 255, i.check = Z(i.check, R, 2, 0)), u = l = 0, i.mode = 5;
                        case 5:
                            if (1024 & i.flags) {
                                for (; u < 16;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                i.length = l, i.head && (i.head.extra_len = l), 512 & i.flags && (R[0] = 255 & l, R[1] = l >>> 8 & 255, i.check = Z(i.check, R, 2, 0)), u = l = 0
                            } else i.head && (i.head.extra = null);
                            i.mode = 6;
                        case 6:
                            if (1024 & i.flags && (c = i.length, s < c && (c = s), c && (i.head && (y = i.head.extra_len - i.length, i.head.extra || (i.head.extra = new Array(i.head.extra_len)), B.arraySet(i.head.extra, n, a, c, y)), 512 & i.flags && (i.check = Z(i.check, n, c, a)), s -= c, a += c, i.length -= c), i.length)) break e;
                            i.length = 0, i.mode = 7;
                        case 7:
                            if (2048 & i.flags) {
                                if (0 === s) break e;
                                for (c = 0; y = n[a + c++], i.head && y && i.length < 65536 && (i.head.name += String.fromCharCode(y)), y && c < s;) ;
                                if (512 & i.flags && (i.check = Z(i.check, n, c, a)), s -= c, a += c, y) break e
                            } else i.head && (i.head.name = null);
                            i.length = 0, i.mode = 8;
                        case 8:
                            if (4096 & i.flags) {
                                if (0 === s) break e;
                                for (c = 0; y = n[a + c++], i.head && y && i.length < 65536 && (i.head.comment += String.fromCharCode(y)), y && c < s;) ;
                                if (512 & i.flags && (i.check = Z(i.check, n, c, a)), s -= c, a += c, y) break e
                            } else i.head && (i.head.comment = null);
                            i.mode = 9;
                        case 9:
                            if (512 & i.flags) {
                                for (; u < 16;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                if (l !== (65535 & i.check)) {
                                    e.msg = "header crc mismatch", i.mode = 30;
                                    break
                                }
                                u = l = 0
                            }
                            i.head && (i.head.hcrc = i.flags >> 9 & 1, i.head.done = !0), e.adler = i.check = 0, i.mode = 12;
                            break;
                        case 10:
                            for (; u < 32;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            e.adler = i.check = L(l), u = l = 0, i.mode = 11;
                        case 11:
                            if (0 === i.havedict) return e.next_out = o, e.avail_out = f, e.next_in = a, e.avail_in = s, i.hold = l, i.bits = u, 2;
                            e.adler = i.check = 1, i.mode = 12;
                        case 12:
                            if (5 === t || 6 === t) break e;
                        case 13:
                            if (i.last) {
                                l >>>= 7 & u, u -= 7 & u, i.mode = 27;
                                break
                            }
                            for (; u < 3;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            switch (i.last = 1 & l, --u, 3 & (l >>>= 1)) {
                                case 0:
                                    i.mode = 14;
                                    break;
                                case 1:
                                    if (H(i), i.mode = 20, 6 !== t) break;
                                    l >>>= 2, u -= 2;
                                    break e;
                                case 2:
                                    i.mode = 17;
                                    break;
                                case 3:
                                    e.msg = "invalid block type", i.mode = 30
                            }
                            l >>>= 2, u -= 2;
                            break;
                        case 14:
                            for (l >>>= 7 & u, u -= 7 & u; u < 32;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if ((65535 & l) != (l >>> 16 ^ 65535)) {
                                e.msg = "invalid stored block lengths", i.mode = 30;
                                break
                            }
                            if (i.length = 65535 & l, u = l = 0, i.mode = 15, 6 === t) break e;
                        case 15:
                            i.mode = 16;
                        case 16:
                            if (c = i.length) {
                                if (s < c && (c = s), f < c && (c = f), 0 === c) break e;
                                B.arraySet(r, n, a, c, o), s -= c, a += c, f -= c, o += c, i.length -= c;
                                break
                            }
                            i.mode = 12;
                            break;
                        case 17:
                            for (; u < 14;) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if (i.nlen = 257 + (31 & l), l >>>= 5, u -= 5, i.ndist = 1 + (31 & l), l >>>= 5, u -= 5, i.ncode = 4 + (15 & l), l >>>= 4, u -= 4, 286 < i.nlen || 30 < i.ndist) {
                                e.msg = "too many length or distance symbols", i.mode = 30;
                                break
                            }
                            i.have = 0, i.mode = 18;
                        case 18:
                            for (; i.have < i.ncode;) {
                                for (; u < 3;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                i.lens[A[i.have++]] = 7 & l, l >>>= 3, u -= 3
                            }
                            for (; i.have < 19;) i.lens[A[i.have++]] = 0;
                            if (i.lencode = i.lendyn, i.lenbits = 7, E = {bits: i.lenbits}, x = D(0, i.lens, 0, 19, i.lencode, 0, i.work, E), i.lenbits = E.bits, x) {
                                e.msg = "invalid code lengths set", i.mode = 30;
                                break
                            }
                            i.have = 0, i.mode = 19;
                        case 19:
                            for (; i.have < i.nlen + i.ndist;) {
                                for (; T = i.lencode[l & (1 << i.lenbits) - 1], g = T >>> 16 & 255, v = 65535 & T, !((w = T >>> 24) <= u);) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                if (v < 16) l >>>= w, u -= w, i.lens[i.have++] = v; else {
                                    if (16 === v) {
                                        for (S = w + 2; u < S;) {
                                            if (0 === s) break e;
                                            s--, l += n[a++] << u, u += 8
                                        }
                                        if (l >>>= w, u -= w, 0 === i.have) {
                                            e.msg = "invalid bit length repeat", i.mode = 30;
                                            break
                                        }
                                        y = i.lens[i.have - 1], c = 3 + (3 & l), l >>>= 2, u -= 2
                                    } else if (17 === v) {
                                        for (S = w + 3; u < S;) {
                                            if (0 === s) break e;
                                            s--, l += n[a++] << u, u += 8
                                        }
                                        u -= w, y = 0, c = 3 + (7 & (l >>>= w)), l >>>= 3, u -= 3
                                    } else {
                                        for (S = w + 7; u < S;) {
                                            if (0 === s) break e;
                                            s--, l += n[a++] << u, u += 8
                                        }
                                        u -= w, y = 0, c = 11 + (127 & (l >>>= w)), l >>>= 7, u -= 7
                                    }
                                    if (i.have + c > i.nlen + i.ndist) {
                                        e.msg = "invalid bit length repeat", i.mode = 30;
                                        break
                                    }
                                    for (; c--;) i.lens[i.have++] = y
                                }
                            }
                            if (30 === i.mode) break;
                            if (0 === i.lens[256]) {
                                e.msg = "invalid code -- missing end-of-block", i.mode = 30;
                                break
                            }
                            if (i.lenbits = 9, E = {bits: i.lenbits}, x = D(N, i.lens, 0, i.nlen, i.lencode, 0, i.work, E), i.lenbits = E.bits, x) {
                                e.msg = "invalid literal/lengths set", i.mode = 30;
                                break
                            }
                            if (i.distbits = 6, i.distcode = i.distdyn, E = {bits: i.distbits}, x = D(O, i.lens, i.nlen, i.ndist, i.distcode, 0, i.work, E), i.distbits = E.bits, x) {
                                e.msg = "invalid distances set", i.mode = 30;
                                break
                            }
                            if (i.mode = 20, 6 === t) break e;
                        case 20:
                            i.mode = 21;
                        case 21:
                            if (6 <= s && 258 <= f) {
                                e.next_out = o, e.avail_out = f, e.next_in = a, e.avail_in = s, i.hold = l, i.bits = u, I(e, h), o = e.next_out, r = e.output, f = e.avail_out, a = e.next_in, n = e.input, s = e.avail_in, l = i.hold, u = i.bits, 12 === i.mode && (i.back = -1);
                                break
                            }
                            for (i.back = 0; T = i.lencode[l & (1 << i.lenbits) - 1], g = T >>> 16 & 255, v = 65535 & T, !((w = T >>> 24) <= u);) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if (g && 0 == (240 & g)) {
                                for (k = w, _ = g, p = v; T = i.lencode[p + ((l & (1 << k + _) - 1) >> k)], g = T >>> 16 & 255, v = 65535 & T, !(k + (w = T >>> 24) <= u);) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                l >>>= k, u -= k, i.back += k
                            }
                            if (l >>>= w, u -= w, i.back += w, i.length = v, 0 === g) {
                                i.mode = 26;
                                break
                            }
                            if (32 & g) {
                                i.back = -1, i.mode = 12;
                                break
                            }
                            if (64 & g) {
                                e.msg = "invalid literal/length code", i.mode = 30;
                                break
                            }
                            i.extra = 15 & g, i.mode = 22;
                        case 22:
                            if (i.extra) {
                                for (S = i.extra; u < S;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                i.length += l & (1 << i.extra) - 1, l >>>= i.extra, u -= i.extra, i.back += i.extra
                            }
                            i.was = i.length, i.mode = 23;
                        case 23:
                            for (; T = i.distcode[l & (1 << i.distbits) - 1], g = T >>> 16 & 255, v = 65535 & T, !((w = T >>> 24) <= u);) {
                                if (0 === s) break e;
                                s--, l += n[a++] << u, u += 8
                            }
                            if (0 == (240 & g)) {
                                for (k = w, _ = g, p = v; T = i.distcode[p + ((l & (1 << k + _) - 1) >> k)], g = T >>> 16 & 255, v = 65535 & T, !(k + (w = T >>> 24) <= u);) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                l >>>= k, u -= k, i.back += k
                            }
                            if (l >>>= w, u -= w, i.back += w, 64 & g) {
                                e.msg = "invalid distance code", i.mode = 30;
                                break
                            }
                            i.offset = v, i.extra = 15 & g, i.mode = 24;
                        case 24:
                            if (i.extra) {
                                for (S = i.extra; u < S;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                i.offset += l & (1 << i.extra) - 1, l >>>= i.extra, u -= i.extra, i.back += i.extra
                            }
                            if (i.offset > i.dmax) {
                                e.msg = "invalid distance too far back", i.mode = 30;
                                break
                            }
                            i.mode = 25;
                        case 25:
                            if (0 === f) break e;
                            if (c = h - f, i.offset > c) {
                                if ((c = i.offset - c) > i.whave && i.sane) {
                                    e.msg = "invalid distance too far back", i.mode = 30;
                                    break
                                }
                                b = c > i.wnext ? (c -= i.wnext, i.wsize - c) : i.wnext - c, c > i.length && (c = i.length), m = i.window
                            } else m = r, b = o - i.offset, c = i.length;
                            for (f < c && (c = f), f -= c, i.length -= c; r[o++] = m[b++], --c;) ;
                            0 === i.length && (i.mode = 21);
                            break;
                        case 26:
                            if (0 === f) break e;
                            r[o++] = i.length, f--, i.mode = 21;
                            break;
                        case 27:
                            if (i.wrap) {
                                for (; u < 32;) {
                                    if (0 === s) break e;
                                    s--, l |= n[a++] << u, u += 8
                                }
                                if (h -= f, e.total_out += h, i.total += h, h && (e.adler = i.check = (i.flags ? Z : U)(i.check, r, h, o - h)), h = f, (i.flags ? l : L(l)) !== i.check) {
                                    e.msg = "incorrect data check", i.mode = 30;
                                    break
                                }
                                u = l = 0
                            }
                            i.mode = 28;
                        case 28:
                            if (i.wrap && i.flags) {
                                for (; u < 32;) {
                                    if (0 === s) break e;
                                    s--, l += n[a++] << u, u += 8
                                }
                                if (l !== (4294967295 & i.total)) {
                                    e.msg = "incorrect length check", i.mode = 30;
                                    break
                                }
                                u = l = 0
                            }
                            i.mode = 29;
                        case 29:
                            x = 1;
                            break e;
                        case 30:
                            x = -3;
                            break e;
                        case 31:
                            return -4;
                        case 32:
                        default:
                            return C
                    }
                    e.next_out = o, e.avail_out = f, e.next_in = a, e.avail_in = s, i.hold = l, i.bits = u, (i.wsize || h !== e.avail_out && i.mode < 30 && (i.mode < 27 || 4 !== t)) && P(e, e.output, e.next_out, h - e.avail_out);
                    d -= e.avail_in, h -= e.avail_out, e.total_in += d, e.total_out += h, i.total += h, i.wrap && h && (e.adler = i.check = (i.flags ? Z : U)(i.check, r, h, e.next_out - h));
                    e.data_type = i.bits + (i.last ? 64 : 0) + (12 === i.mode ? 128 : 0) + (20 === i.mode || 15 === i.mode ? 256 : 0), (0 == d && 0 === h || 4 === t) && x === z && (x = -5);
                    return x
                }, i.inflateEnd = function (e) {
                    if (!e || !e.state) return C;
                    var t = e.state;
                    t.window && (t.window = null);
                    return e.state = null, z
                }, i.inflateGetHeader = function (e, t) {
                    var i;
                    return e && e.state && 0 != (2 & (i = e.state).wrap) ? ((i.head = t).done = !1, z) : C
                }, i.inflateSetDictionary = function (e, t) {
                    var i, n = t.length;
                    if (!e || !e.state) return C;
                    if (0 !== (i = e.state).wrap && 11 !== i.mode) return C;
                    if (11 === i.mode && U(1, t, n, 0) !== i.check) return -3;
                    if (P(e, t, n, n)) return i.mode = 31, -4;
                    return i.havedict = 1, z
                }, i.inflateInfo = "pako inflate (from Nodeca project)"
            }, {"../utils/common": 1, "./adler32": 3, "./crc32": 5, "./inffast": 7, "./inftrees": 9}],
            9: [function (e, t, i) {
                var N = e("../utils/common"),
                    O = [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0],
                    z = [16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 16, 72, 78],
                    C = [1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0],
                    F = [16, 16, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 23, 24, 24, 25, 25, 26, 26, 27, 27, 28, 28, 29, 29, 64, 64];
                t.exports = function (e, t, i, n, r, a, o, s) {
                    var f, l, u, d, h, c, b, m, w, g = s.bits, v = 0, k = 0, _ = 0, p = 0, y = 0, x = 0, E = 0, S = 0,
                        T = 0, R = 0, A = null, B = 0, U = new N.Buf16(16), Z = new N.Buf16(16), I = null, D = 0;
                    for (v = 0; v <= 15; v++) U[v] = 0;
                    for (k = 0; k < n; k++) U[t[i + k]]++;
                    for (y = g, p = 15; 1 <= p && 0 === U[p]; p--) ;
                    if (p < y && (y = p), 0 === p) return r[a++] = 20971520, r[a++] = 20971520, s.bits = 1, 0;
                    for (_ = 1; _ < p && 0 === U[_]; _++) ;
                    for (y < _ && (y = _), v = S = 1; v <= 15; v++) if (S <<= 1, (S -= U[v]) < 0) return -1;
                    if (0 < S && (0 === e || 1 !== p)) return -1;
                    for (Z[1] = 0, v = 1; v < 15; v++) Z[v + 1] = Z[v] + U[v];
                    for (k = 0; k < n; k++) 0 !== t[i + k] && (o[Z[t[i + k]]++] = k);
                    if (c = 0 === e ? (A = I = o, 19) : 1 === e ? (A = O, B -= 257, I = z, D -= 257, 256) : (A = C, I = F, -1), v = _, h = a, E = k = R = 0, u = -1, d = (T = 1 << (x = y)) - 1, 1 === e && 852 < T || 2 === e && 592 < T) return 1;
                    for (; ;) {
                        for (b = v - E, w = o[k] < c ? (m = 0, o[k]) : o[k] > c ? (m = I[D + o[k]], A[B + o[k]]) : (m = 96, 0), f = 1 << v - E, _ = l = 1 << x; r[h + (R >> E) + (l -= f)] = b << 24 | m << 16 | w | 0, 0 !== l;) ;
                        for (f = 1 << v - 1; R & f;) f >>= 1;
                        if (0 !== f ? (R &= f - 1, R += f) : R = 0, k++, 0 == --U[v]) {
                            if (v === p) break;
                            v = t[i + o[k]]
                        }
                        if (y < v && (R & d) !== u) {
                            for (0 === E && (E = y), h += _, S = 1 << (x = v - E); x + E < p && !((S -= U[x + E]) <= 0);) x++, S <<= 1;
                            if (T += 1 << x, 1 === e && 852 < T || 2 === e && 592 < T) return 1;
                            r[u = R & d] = y << 24 | x << 16 | h - a | 0
                        }
                    }
                    return 0 !== R && (r[h + R] = v - E << 24 | 64 << 16 | 0), s.bits = y, 0
                }
            }, {"../utils/common": 1}],
            10: [function (e, t, i) {
                t.exports = {
                    2: "need dictionary",
                    1: "stream end",
                    0: "",
                    "-1": "file error",
                    "-2": "stream error",
                    "-3": "data error",
                    "-4": "insufficient memory",
                    "-5": "buffer error",
                    "-6": "incompatible version"
                }
            }, {}],
            11: [function (e, t, i) {
                t.exports = function () {
                    this.input = null, this.next_in = 0, this.avail_in = 0, this.total_in = 0, this.output = null, this.next_out = 0, this.avail_out = 0, this.total_out = 0, this.msg = "", this.state = null, this.data_type = 2, this.adler = 0
                }
            }, {}],
            "/lib/inflate.js": [function (e, t, i) {
                var h = e("./zlib/inflate"), c = e("./utils/common"), b = e("./utils/strings"), m = e("./zlib/constants"),
                    n = e("./zlib/messages"), r = e("./zlib/zstream"), a = e("./zlib/gzheader"),
                    w = Object.prototype.toString;

                function o(e) {
                    if (!(this instanceof o)) return new o(e);
                    this.options = c.assign({chunkSize: 16384, windowBits: 0, to: ""}, e || {});
                    var t = this.options;
                    t.raw && 0 <= t.windowBits && t.windowBits < 16 && (t.windowBits = -t.windowBits, 0 === t.windowBits && (t.windowBits = -15)), !(0 <= t.windowBits && t.windowBits < 16) || e && e.windowBits || (t.windowBits += 32), 15 < t.windowBits && t.windowBits < 48 && 0 == (15 & t.windowBits) && (t.windowBits |= 15), this.err = 0, this.msg = "", this.ended = !1, this.chunks = [], this.strm = new r, this.strm.avail_out = 0;
                    var i = h.inflateInit2(this.strm, t.windowBits);
                    if (i !== m.Z_OK) throw new Error(n[i]);
                    this.header = new a, h.inflateGetHeader(this.strm, this.header)
                }

                function s(e, t) {
                    var i = new o(t);
                    if (i.push(e, !0), i.err) throw i.msg || n[i.err];
                    return i.result
                }

                o.prototype.push = function (e, t) {
                    var i, n, r, a, o, s, f = this.strm, l = this.options.chunkSize, u = this.options.dictionary, d = !1;
                    if (this.ended) return !1;
                    n = t === ~~t ? t : !0 === t ? m.Z_FINISH : m.Z_NO_FLUSH, "string" == typeof e ? f.input = b.binstring2buf(e) : "[object ArrayBuffer]" === w.call(e) ? f.input = new Uint8Array(e) : f.input = e, f.next_in = 0, f.avail_in = f.input.length;
                    do {
                        if (0 === f.avail_out && (f.output = new c.Buf8(l), f.next_out = 0, f.avail_out = l), (i = h.inflate(f, m.Z_NO_FLUSH)) === m.Z_NEED_DICT && u && (s = "string" == typeof u ? b.string2buf(u) : "[object ArrayBuffer]" === w.call(u) ? new Uint8Array(u) : u, i = h.inflateSetDictionary(this.strm, s)), i === m.Z_BUF_ERROR && !0 === d && (i = m.Z_OK, d = !1), i !== m.Z_STREAM_END && i !== m.Z_OK) return this.onEnd(i), !(this.ended = !0);
                        f.next_out && (0 !== f.avail_out && i !== m.Z_STREAM_END && (0 !== f.avail_in || n !== m.Z_FINISH && n !== m.Z_SYNC_FLUSH) || ("string" === this.options.to ? (r = b.utf8border(f.output, f.next_out), a = f.next_out - r, o = b.buf2string(f.output, r), f.next_out = a, f.avail_out = l - a, a && c.arraySet(f.output, f.output, r, a, 0), this.onData(o)) : this.onData(c.shrinkBuf(f.output, f.next_out)))), 0 === f.avail_in && 0 === f.avail_out && (d = !0)
                    } while ((0 < f.avail_in || 0 === f.avail_out) && i !== m.Z_STREAM_END);
                    return i === m.Z_STREAM_END && (n = m.Z_FINISH), n === m.Z_FINISH ? (i = h.inflateEnd(this.strm), this.onEnd(i), this.ended = !0, i === m.Z_OK) : n !== m.Z_SYNC_FLUSH || (this.onEnd(m.Z_OK), !(f.avail_out = 0))
                }, o.prototype.onData = function (e) {
                    this.chunks.push(e)
                }, o.prototype.onEnd = function (e) {
                    e === m.Z_OK && ("string" === this.options.to ? this.result = this.chunks.join("") : this.result = c.flattenChunks(this.chunks)), this.chunks = [], this.err = e, this.msg = this.strm.msg
                }, i.Inflate = o, i.inflate = s, i.inflateRaw = function (e, t) {
                    return (t = t || {}).raw = !0, s(e, t)
                }, i.ungzip = s
            }, {
                "./utils/common": 1,
                "./utils/strings": 2,
                "./zlib/constants": 4,
                "./zlib/gzheader": 6,
                "./zlib/inflate": 8,
                "./zlib/messages": 10,
                "./zlib/zstream": 11
            }]
        }, {}, [])("/lib/inflate.js"), R = Uint16Array.BYTES_PER_ELEMENT, A = Int32Array.BYTES_PER_ELEMENT,
        B = Uint32Array.BYTES_PER_ELEMENT, o = {METADATA: 0, TERRAIN: 1, DBROOT: 2};
    o.fromString = function (e) {
        return "Metadata" === e ? o.METADATA : "Terrain" === e ? o.TERRAIN : "DbRoot" === e ? o.DBROOT : void 0
    };
    var U = 32301;
    var f = 1953029805, l = 2917034100;
    return t(function (e, t) {
        var i = o.fromString(e.type), n = e.buffer;
        b(e.key, n);
        var r = function (e) {
            var t = new DataView(e), i = 0, n = t.getUint32(i, !0);
            if (i += B, n !== f && n !== l) throw new S.RuntimeError("Invalid magic");
            var r = t.getUint32(i, n === f);
            i += B;
            var a = new Uint8Array(e, i), o = s.inflate(a);
            if (o.length === r) return o;
            throw new S.RuntimeError("Size of packet doesn't match header")
        }(n);
        n = r.buffer;
        var a = r.length;
        switch (i) {
            case o.METADATA:
                return function (e, t, i) {
                    var n = new DataView(e), r = 0, a = n.getUint32(r, !0);
                    if (r += B, a !== U) throw new S.RuntimeError("Invalid magic");
                    var o = n.getUint32(r, !0);
                    if (r += B, 1 !== o) throw new S.RuntimeError("Invalid data type. Must be 1 for QuadTreePacket");
                    var s = n.getUint32(r, !0);
                    if (r += B, 2 !== s) throw new S.RuntimeError("Invalid QuadTreePacket version. Only version 2 is supported.");
                    var f = n.getInt32(r, !0);
                    r += A;
                    var l = n.getInt32(r, !0);
                    if (r += A, 32 !== l) throw new S.RuntimeError("Invalid instance size.");
                    var u = n.getInt32(r, !0);
                    r += A;
                    var d = n.getInt32(r, !0);
                    r += A;
                    var h = n.getInt32(r, !0);
                    if (u !== f * l + (r += A)) throw new S.RuntimeError("Invalid dataBufferOffset");
                    if (u + d + h !== t) throw new S.RuntimeError("Invalid packet offsets");
                    for (var c = [], b = 0; b < f; ++b) {
                        var m = n.getUint8(r);
                        ++r, ++r;
                        var w = n.getUint16(r, !0);
                        r += R;
                        var g = n.getUint16(r, !0);
                        r += R;
                        var v = n.getUint16(r, !0);
                        r += R, r += R, r += R, r += A, r += A, r += 8;
                        var k = n.getUint8(r++), _ = n.getUint8(r++);
                        r += R, c.push(new T(m, w, g, v, k, _))
                    }
                    var p = [], y = 0;
                    var x = 0, E = c[y++];
                    "" === i ? ++x : p[i] = E;
                    return function e(t, i, n) {
                        var r = !1;
                        if (4 === n) {
                            if (i.hasSubtree()) return;
                            r = !0
                        }
                        for (var a = 0; a < 4; ++a) {
                            var o = t + a.toString();
                            if (r) p[o] = null; else if (n < 4) if (i.hasChild(a)) {
                                if (y === f) return void console.log("Incorrect number of instances");
                                var s = c[y++];
                                p[o] = s, e(o, s, n + 1)
                            } else p[o] = null
                        }
                    }(i, E, x), p
                }(n, a, e.quadKey);
            case o.TERRAIN:
                return function (e, t, i) {
                    var n = new DataView(e), r = 0, a = [];
                    for (; r < t;) {
                        for (var o = r, s = 0; s < 4; ++s) {
                            var f = n.getUint32(r, !0);
                            r += B, r += f
                        }
                        var l = e.slice(o, r);
                        i.push(l), a.push(l)
                    }
                    return a
                }(n, a, t);
            case o.DBROOT:
                return t.push(n), {buffer: n}
        }
    })
});
