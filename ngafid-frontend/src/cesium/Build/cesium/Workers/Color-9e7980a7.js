define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Transforms-44592b02"], function (e, O, r, g, o) {
    "use strict";

    function a(e, r, o) {
        return o < 0 && (o += 1), 1 < o && --o, 6 * o < 1 ? e + 6 * (r - e) * o : 2 * o < 1 ? r : 3 * o < 2 ? e + (r - e) * (2 / 3 - o) * 6 : e
    }

    function F(e, r, o, t) {
        this.red = O.defaultValue(e, 1), this.green = O.defaultValue(r, 1), this.blue = O.defaultValue(o, 1), this.alpha = O.defaultValue(t, 1)
    }

    var t, f, s;
    F.fromCartesian4 = function (e, r) {
        return O.defined(r) ? (r.red = e.x, r.green = e.y, r.blue = e.z, r.alpha = e.w, r) : new F(e.x, e.y, e.z, e.w)
    }, F.fromBytes = function (e, r, o, t, f) {
        return e = F.byteToFloat(O.defaultValue(e, 255)), r = F.byteToFloat(O.defaultValue(r, 255)), o = F.byteToFloat(O.defaultValue(o, 255)), t = F.byteToFloat(O.defaultValue(t, 255)), O.defined(f) ? (f.red = e, f.green = r, f.blue = o, f.alpha = t, f) : new F(e, r, o, t)
    }, F.fromAlpha = function (e, r, o) {
        return O.defined(o) ? (o.red = e.red, o.green = e.green, o.blue = e.blue, o.alpha = r, o) : new F(e.red, e.green, e.blue, r)
    }, o.FeatureDetection.supportsTypedArrays() && (t = new ArrayBuffer(4), f = new Uint32Array(t), s = new Uint8Array(t)), F.fromRgba = function (e, r) {
        return f[0] = e, F.fromBytes(s[0], s[1], s[2], s[3], r)
    }, F.fromHsl = function (e, r, o, t, f) {
        e = O.defaultValue(e, 0) % 1, r = O.defaultValue(r, 0), o = O.defaultValue(o, 0), t = O.defaultValue(t, 1);
        var s = o, n = o, C = o;
        if (0 !== r) {
            var l, i = 2 * o - (l = o < .5 ? o * (1 + r) : o + r - o * r);
            s = a(i, l, e + 1 / 3), n = a(i, l, e), C = a(i, l, e - 1 / 3)
        }
        return O.defined(f) ? (f.red = s, f.green = n, f.blue = C, f.alpha = t, f) : new F(s, n, C, t)
    }, F.fromRandom = function (e, r) {
        var o = (e = O.defaultValue(e, O.defaultValue.EMPTY_OBJECT)).red;
        if (!O.defined(o)) {
            var t = O.defaultValue(e.minimumRed, 0), f = O.defaultValue(e.maximumRed, 1);
            o = t + g.CesiumMath.nextRandomNumber() * (f - t)
        }
        var s = e.green;
        if (!O.defined(s)) {
            var n = O.defaultValue(e.minimumGreen, 0), C = O.defaultValue(e.maximumGreen, 1);
            s = n + g.CesiumMath.nextRandomNumber() * (C - n)
        }
        var l = e.blue;
        if (!O.defined(l)) {
            var i = O.defaultValue(e.minimumBlue, 0), a = O.defaultValue(e.maximumBlue, 1);
            l = i + g.CesiumMath.nextRandomNumber() * (a - i)
        }
        var E = e.alpha;
        if (!O.defined(E)) {
            var b = O.defaultValue(e.minimumAlpha, 0), u = O.defaultValue(e.maximumAlpha, 1);
            E = b + g.CesiumMath.nextRandomNumber() * (u - b)
        }
        return O.defined(r) ? (r.red = o, r.green = s, r.blue = l, r.alpha = E, r) : new F(o, s, l, E)
    };
    var n = /^#([0-9a-f])([0-9a-f])([0-9a-f])$/i, C = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i,
        l = /^rgba?\(\s*([0-9.]+%?)\s*,\s*([0-9.]+%?)\s*,\s*([0-9.]+%?)(?:\s*,\s*([0-9.]+))?\s*\)$/i,
        i = /^hsla?\(\s*([0-9.]+)\s*,\s*([0-9.]+%)\s*,\s*([0-9.]+%)(?:\s*,\s*([0-9.]+))?\s*\)$/i;
    F.fromCssColorString = function (e, r) {
        O.defined(r) || (r = new F);
        var o = F[e.toUpperCase()];
        if (O.defined(o)) return F.clone(o, r), r;
        var t = n.exec(e);
        return null !== t ? (r.red = parseInt(t[1], 16) / 15, r.green = parseInt(t[2], 16) / 15, r.blue = parseInt(t[3], 16) / 15, r.alpha = 1, r) : null !== (t = C.exec(e)) ? (r.red = parseInt(t[1], 16) / 255, r.green = parseInt(t[2], 16) / 255, r.blue = parseInt(t[3], 16) / 255, r.alpha = 1, r) : null !== (t = l.exec(e)) ? (r.red = parseFloat(t[1]) / ("%" === t[1].substr(-1) ? 100 : 255), r.green = parseFloat(t[2]) / ("%" === t[2].substr(-1) ? 100 : 255), r.blue = parseFloat(t[3]) / ("%" === t[3].substr(-1) ? 100 : 255), r.alpha = parseFloat(O.defaultValue(t[4], "1.0")), r) : null !== (t = i.exec(e)) ? F.fromHsl(parseFloat(t[1]) / 360, parseFloat(t[2]) / 100, parseFloat(t[3]) / 100, parseFloat(O.defaultValue(t[4], "1.0")), r) : r = void 0
    }, F.packedLength = 4, F.pack = function (e, r, o) {
        return o = O.defaultValue(o, 0), r[o++] = e.red, r[o++] = e.green, r[o++] = e.blue, r[o] = e.alpha, r
    }, F.unpack = function (e, r, o) {
        return r = O.defaultValue(r, 0), O.defined(o) || (o = new F), o.red = e[r++], o.green = e[r++], o.blue = e[r++], o.alpha = e[r], o
    }, F.byteToFloat = function (e) {
        return e / 255
    }, F.floatToByte = function (e) {
        return 1 === e ? 255 : 256 * e | 0
    }, F.clone = function (e, r) {
        if (O.defined(e)) return O.defined(r) ? (r.red = e.red, r.green = e.green, r.blue = e.blue, r.alpha = e.alpha, r) : new F(e.red, e.green, e.blue, e.alpha)
    }, F.equals = function (e, r) {
        return e === r || O.defined(e) && O.defined(r) && e.red === r.red && e.green === r.green && e.blue === r.blue && e.alpha === r.alpha
    }, F.equalsArray = function (e, r, o) {
        return e.red === r[o] && e.green === r[o + 1] && e.blue === r[o + 2] && e.alpha === r[o + 3]
    }, F.prototype.clone = function (e) {
        return F.clone(this, e)
    }, F.prototype.equals = function (e) {
        return F.equals(this, e)
    }, F.prototype.equalsEpsilon = function (e, r) {
        return this === e || O.defined(e) && Math.abs(this.red - e.red) <= r && Math.abs(this.green - e.green) <= r && Math.abs(this.blue - e.blue) <= r && Math.abs(this.alpha - e.alpha) <= r
    }, F.prototype.toString = function () {
        return "(" + this.red + ", " + this.green + ", " + this.blue + ", " + this.alpha + ")"
    }, F.prototype.toCssColorString = function () {
        var e = F.floatToByte(this.red), r = F.floatToByte(this.green), o = F.floatToByte(this.blue);
        return 1 === this.alpha ? "rgb(" + e + "," + r + "," + o + ")" : "rgba(" + e + "," + r + "," + o + "," + this.alpha + ")"
    }, F.prototype.toBytes = function (e) {
        var r = F.floatToByte(this.red), o = F.floatToByte(this.green), t = F.floatToByte(this.blue),
            f = F.floatToByte(this.alpha);
        return O.defined(e) ? (e[0] = r, e[1] = o, e[2] = t, e[3] = f, e) : [r, o, t, f]
    }, F.prototype.toRgba = function () {
        return s[0] = F.floatToByte(this.red), s[1] = F.floatToByte(this.green), s[2] = F.floatToByte(this.blue), s[3] = F.floatToByte(this.alpha), f[0]
    }, F.prototype.brighten = function (e, r) {
        return e = 1 - e, r.red = 1 - (1 - this.red) * e, r.green = 1 - (1 - this.green) * e, r.blue = 1 - (1 - this.blue) * e, r.alpha = this.alpha, r
    }, F.prototype.darken = function (e, r) {
        return e = 1 - e, r.red = this.red * e, r.green = this.green * e, r.blue = this.blue * e, r.alpha = this.alpha, r
    }, F.prototype.withAlpha = function (e, r) {
        return F.fromAlpha(this, e, r)
    }, F.add = function (e, r, o) {
        return o.red = e.red + r.red, o.green = e.green + r.green, o.blue = e.blue + r.blue, o.alpha = e.alpha + r.alpha, o
    }, F.subtract = function (e, r, o) {
        return o.red = e.red - r.red, o.green = e.green - r.green, o.blue = e.blue - r.blue, o.alpha = e.alpha - r.alpha, o
    }, F.multiply = function (e, r, o) {
        return o.red = e.red * r.red, o.green = e.green * r.green, o.blue = e.blue * r.blue, o.alpha = e.alpha * r.alpha, o
    }, F.divide = function (e, r, o) {
        return o.red = e.red / r.red, o.green = e.green / r.green, o.blue = e.blue / r.blue, o.alpha = e.alpha / r.alpha, o
    }, F.mod = function (e, r, o) {
        return o.red = e.red % r.red, o.green = e.green % r.green, o.blue = e.blue % r.blue, o.alpha = e.alpha % r.alpha, o
    }, F.lerp = function (e, r, o, t) {
        return t.red = g.CesiumMath.lerp(e.red, r.red, o), t.green = g.CesiumMath.lerp(e.green, r.green, o), t.blue = g.CesiumMath.lerp(e.blue, r.blue, o), t.alpha = g.CesiumMath.lerp(e.alpha, r.alpha, o), t
    }, F.multiplyByScalar = function (e, r, o) {
        return o.red = e.red * r, o.green = e.green * r, o.blue = e.blue * r, o.alpha = e.alpha * r, o
    }, F.divideByScalar = function (e, r, o) {
        return o.red = e.red / r, o.green = e.green / r, o.blue = e.blue / r, o.alpha = e.alpha / r, o
    }, F.ALICEBLUE = Object.freeze(F.fromCssColorString("#F0F8FF")), F.ANTIQUEWHITE = Object.freeze(F.fromCssColorString("#FAEBD7")), F.AQUA = Object.freeze(F.fromCssColorString("#00FFFF")), F.AQUAMARINE = Object.freeze(F.fromCssColorString("#7FFFD4")), F.AZURE = Object.freeze(F.fromCssColorString("#F0FFFF")), F.BEIGE = Object.freeze(F.fromCssColorString("#F5F5DC")), F.BISQUE = Object.freeze(F.fromCssColorString("#FFE4C4")), F.BLACK = Object.freeze(F.fromCssColorString("#000000")), F.BLANCHEDALMOND = Object.freeze(F.fromCssColorString("#FFEBCD")), F.BLUE = Object.freeze(F.fromCssColorString("#0000FF")), F.BLUEVIOLET = Object.freeze(F.fromCssColorString("#8A2BE2")), F.BROWN = Object.freeze(F.fromCssColorString("#A52A2A")), F.BURLYWOOD = Object.freeze(F.fromCssColorString("#DEB887")), F.CADETBLUE = Object.freeze(F.fromCssColorString("#5F9EA0")), F.CHARTREUSE = Object.freeze(F.fromCssColorString("#7FFF00")), F.CHOCOLATE = Object.freeze(F.fromCssColorString("#D2691E")), F.CORAL = Object.freeze(F.fromCssColorString("#FF7F50")), F.CORNFLOWERBLUE = Object.freeze(F.fromCssColorString("#6495ED")), F.CORNSILK = Object.freeze(F.fromCssColorString("#FFF8DC")), F.CRIMSON = Object.freeze(F.fromCssColorString("#DC143C")), F.CYAN = Object.freeze(F.fromCssColorString("#00FFFF")), F.DARKBLUE = Object.freeze(F.fromCssColorString("#00008B")), F.DARKCYAN = Object.freeze(F.fromCssColorString("#008B8B")), F.DARKGOLDENROD = Object.freeze(F.fromCssColorString("#B8860B")), F.DARKGRAY = Object.freeze(F.fromCssColorString("#A9A9A9")), F.DARKGREEN = Object.freeze(F.fromCssColorString("#006400")), F.DARKGREY = F.DARKGRAY, F.DARKKHAKI = Object.freeze(F.fromCssColorString("#BDB76B")), F.DARKMAGENTA = Object.freeze(F.fromCssColorString("#8B008B")), F.DARKOLIVEGREEN = Object.freeze(F.fromCssColorString("#556B2F")), F.DARKORANGE = Object.freeze(F.fromCssColorString("#FF8C00")), F.DARKORCHID = Object.freeze(F.fromCssColorString("#9932CC")), F.DARKRED = Object.freeze(F.fromCssColorString("#8B0000")), F.DARKSALMON = Object.freeze(F.fromCssColorString("#E9967A")), F.DARKSEAGREEN = Object.freeze(F.fromCssColorString("#8FBC8F")), F.DARKSLATEBLUE = Object.freeze(F.fromCssColorString("#483D8B")), F.DARKSLATEGRAY = Object.freeze(F.fromCssColorString("#2F4F4F")), F.DARKSLATEGREY = F.DARKSLATEGRAY, F.DARKTURQUOISE = Object.freeze(F.fromCssColorString("#00CED1")), F.DARKVIOLET = Object.freeze(F.fromCssColorString("#9400D3")), F.DEEPPINK = Object.freeze(F.fromCssColorString("#FF1493")), F.DEEPSKYBLUE = Object.freeze(F.fromCssColorString("#00BFFF")), F.DIMGRAY = Object.freeze(F.fromCssColorString("#696969")), F.DIMGREY = F.DIMGRAY, F.DODGERBLUE = Object.freeze(F.fromCssColorString("#1E90FF")), F.FIREBRICK = Object.freeze(F.fromCssColorString("#B22222")), F.FLORALWHITE = Object.freeze(F.fromCssColorString("#FFFAF0")), F.FORESTGREEN = Object.freeze(F.fromCssColorString("#228B22")), F.FUCHSIA = Object.freeze(F.fromCssColorString("#FF00FF")), F.GAINSBORO = Object.freeze(F.fromCssColorString("#DCDCDC")), F.GHOSTWHITE = Object.freeze(F.fromCssColorString("#F8F8FF")), F.GOLD = Object.freeze(F.fromCssColorString("#FFD700")), F.GOLDENROD = Object.freeze(F.fromCssColorString("#DAA520")), F.GRAY = Object.freeze(F.fromCssColorString("#808080")), F.GREEN = Object.freeze(F.fromCssColorString("#008000")), F.GREENYELLOW = Object.freeze(F.fromCssColorString("#ADFF2F")), F.GREY = F.GRAY, F.HONEYDEW = Object.freeze(F.fromCssColorString("#F0FFF0")), F.HOTPINK = Object.freeze(F.fromCssColorString("#FF69B4")), F.INDIANRED = Object.freeze(F.fromCssColorString("#CD5C5C")), F.INDIGO = Object.freeze(F.fromCssColorString("#4B0082")), F.IVORY = Object.freeze(F.fromCssColorString("#FFFFF0")), F.KHAKI = Object.freeze(F.fromCssColorString("#F0E68C")), F.LAVENDER = Object.freeze(F.fromCssColorString("#E6E6FA")), F.LAVENDAR_BLUSH = Object.freeze(F.fromCssColorString("#FFF0F5")), F.LAWNGREEN = Object.freeze(F.fromCssColorString("#7CFC00")), F.LEMONCHIFFON = Object.freeze(F.fromCssColorString("#FFFACD")), F.LIGHTBLUE = Object.freeze(F.fromCssColorString("#ADD8E6")), F.LIGHTCORAL = Object.freeze(F.fromCssColorString("#F08080")), F.LIGHTCYAN = Object.freeze(F.fromCssColorString("#E0FFFF")), F.LIGHTGOLDENRODYELLOW = Object.freeze(F.fromCssColorString("#FAFAD2")), F.LIGHTGRAY = Object.freeze(F.fromCssColorString("#D3D3D3")), F.LIGHTGREEN = Object.freeze(F.fromCssColorString("#90EE90")), F.LIGHTGREY = F.LIGHTGRAY,F.LIGHTPINK = Object.freeze(F.fromCssColorString("#FFB6C1")),F.LIGHTSEAGREEN = Object.freeze(F.fromCssColorString("#20B2AA")),F.LIGHTSKYBLUE = Object.freeze(F.fromCssColorString("#87CEFA")),F.LIGHTSLATEGRAY = Object.freeze(F.fromCssColorString("#778899")),F.LIGHTSLATEGREY = F.LIGHTSLATEGRAY,F.LIGHTSTEELBLUE = Object.freeze(F.fromCssColorString("#B0C4DE")),F.LIGHTYELLOW = Object.freeze(F.fromCssColorString("#FFFFE0")),F.LIME = Object.freeze(F.fromCssColorString("#00FF00")),F.LIMEGREEN = Object.freeze(F.fromCssColorString("#32CD32")),F.LINEN = Object.freeze(F.fromCssColorString("#FAF0E6")),F.MAGENTA = Object.freeze(F.fromCssColorString("#FF00FF")),F.MAROON = Object.freeze(F.fromCssColorString("#800000")),F.MEDIUMAQUAMARINE = Object.freeze(F.fromCssColorString("#66CDAA")),F.MEDIUMBLUE = Object.freeze(F.fromCssColorString("#0000CD")),F.MEDIUMORCHID = Object.freeze(F.fromCssColorString("#BA55D3")),F.MEDIUMPURPLE = Object.freeze(F.fromCssColorString("#9370DB")),F.MEDIUMSEAGREEN = Object.freeze(F.fromCssColorString("#3CB371")),F.MEDIUMSLATEBLUE = Object.freeze(F.fromCssColorString("#7B68EE")),F.MEDIUMSPRINGGREEN = Object.freeze(F.fromCssColorString("#00FA9A")),F.MEDIUMTURQUOISE = Object.freeze(F.fromCssColorString("#48D1CC")),F.MEDIUMVIOLETRED = Object.freeze(F.fromCssColorString("#C71585")),F.MIDNIGHTBLUE = Object.freeze(F.fromCssColorString("#191970")),F.MINTCREAM = Object.freeze(F.fromCssColorString("#F5FFFA")),F.MISTYROSE = Object.freeze(F.fromCssColorString("#FFE4E1")),F.MOCCASIN = Object.freeze(F.fromCssColorString("#FFE4B5")),F.NAVAJOWHITE = Object.freeze(F.fromCssColorString("#FFDEAD")),F.NAVY = Object.freeze(F.fromCssColorString("#000080")),F.OLDLACE = Object.freeze(F.fromCssColorString("#FDF5E6")),F.OLIVE = Object.freeze(F.fromCssColorString("#808000")),F.OLIVEDRAB = Object.freeze(F.fromCssColorString("#6B8E23")),F.ORANGE = Object.freeze(F.fromCssColorString("#FFA500")),F.ORANGERED = Object.freeze(F.fromCssColorString("#FF4500")),F.ORCHID = Object.freeze(F.fromCssColorString("#DA70D6")),F.PALEGOLDENROD = Object.freeze(F.fromCssColorString("#EEE8AA")),F.PALEGREEN = Object.freeze(F.fromCssColorString("#98FB98")),F.PALETURQUOISE = Object.freeze(F.fromCssColorString("#AFEEEE")),F.PALEVIOLETRED = Object.freeze(F.fromCssColorString("#DB7093")),F.PAPAYAWHIP = Object.freeze(F.fromCssColorString("#FFEFD5")),F.PEACHPUFF = Object.freeze(F.fromCssColorString("#FFDAB9")),F.PERU = Object.freeze(F.fromCssColorString("#CD853F")),F.PINK = Object.freeze(F.fromCssColorString("#FFC0CB")),F.PLUM = Object.freeze(F.fromCssColorString("#DDA0DD")),F.POWDERBLUE = Object.freeze(F.fromCssColorString("#B0E0E6")),F.PURPLE = Object.freeze(F.fromCssColorString("#800080")),F.RED = Object.freeze(F.fromCssColorString("#FF0000")),F.ROSYBROWN = Object.freeze(F.fromCssColorString("#BC8F8F")),F.ROYALBLUE = Object.freeze(F.fromCssColorString("#4169E1")),F.SADDLEBROWN = Object.freeze(F.fromCssColorString("#8B4513")),F.SALMON = Object.freeze(F.fromCssColorString("#FA8072")),F.SANDYBROWN = Object.freeze(F.fromCssColorString("#F4A460")),F.SEAGREEN = Object.freeze(F.fromCssColorString("#2E8B57")),F.SEASHELL = Object.freeze(F.fromCssColorString("#FFF5EE")),F.SIENNA = Object.freeze(F.fromCssColorString("#A0522D")),F.SILVER = Object.freeze(F.fromCssColorString("#C0C0C0")),F.SKYBLUE = Object.freeze(F.fromCssColorString("#87CEEB")),F.SLATEBLUE = Object.freeze(F.fromCssColorString("#6A5ACD")),F.SLATEGRAY = Object.freeze(F.fromCssColorString("#708090")),F.SLATEGREY = F.SLATEGRAY,F.SNOW = Object.freeze(F.fromCssColorString("#FFFAFA")),F.SPRINGGREEN = Object.freeze(F.fromCssColorString("#00FF7F")),F.STEELBLUE = Object.freeze(F.fromCssColorString("#4682B4")),F.TAN = Object.freeze(F.fromCssColorString("#D2B48C")),F.TEAL = Object.freeze(F.fromCssColorString("#008080")),F.THISTLE = Object.freeze(F.fromCssColorString("#D8BFD8")),F.TOMATO = Object.freeze(F.fromCssColorString("#FF6347")),F.TURQUOISE = Object.freeze(F.fromCssColorString("#40E0D0")),F.VIOLET = Object.freeze(F.fromCssColorString("#EE82EE")),F.WHEAT = Object.freeze(F.fromCssColorString("#F5DEB3")),F.WHITE = Object.freeze(F.fromCssColorString("#FFFFFF")),F.WHITESMOKE = Object.freeze(F.fromCssColorString("#F5F5F5")),F.YELLOW = Object.freeze(F.fromCssColorString("#FFFF00")),F.YELLOWGREEN = Object.freeze(F.fromCssColorString("#9ACD32")),F.TRANSPARENT = Object.freeze(new F(0, 0, 0, 0)),e.Color = F
});
