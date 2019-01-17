package dotterweide

object ColorUtil {
  private final val Pr  = 0.299
  private final val Pg  = 0.587
  private final val Pb  = 0.114

  final case class HSP(H: Double, S: Double, P: Double)
  final case class RGB(R: Double, G: Double, B: Double)

  // public domain function by Darel Rex Finley, 2006
  // adapted to Scala by Hanns Holger Rutz, 2018
  //
  // This function expects the passed-in values to be on a scale
  // of 0 to 1, and uses that same scale for the return values.
  //
  // See description/examples at alienryderflex.com/hsp.html

  def RGBtoHSP(rgb: RGB): HSP = {
    import rgb._
    // Calculate the Perceived brightness.
    val P = math.sqrt(R*R*Pr + G*G*Pg + B*B*Pb)

    // Calculate the Hue and Saturation. (This part works
    // the same way as in the HSV/B and HSL systems???.)
    if (R == G && R == B) {
      HSP(H = 0.0, S = 0.0, P = P)
    } else if (R >= G && R >= B) {  //  R is largest
      if (B >= G) {
        HSP(H = 6.0/6.0 - 1.0/6.0*(B-G)/(R-G), S = 1.0-G/R, P = P)
      } else {
        HSP(H = 0.0/6.0 + 1.0/6.0*(G-B)/(R-B), S = 1.0-B/R, P = P)
      }
    } else if (G >= R && G >=B ) {  //  G is largest
      if (R >= B) {
        HSP(H = 2.0/6.0 - 1.0/6.0*(R-B)/(G-B), S = 1.0-B/G, P = P)
      } else {
        HSP(H = 2.0/6.0 + 1.0/6.0*(B-R)/(G-R), S = 1.0-R/G, P = P)
      }
    } else {                        //  B is largest
      if (G >= R) {
        HSP(H = 4.0/6.0 - 1.0/6.0*(G-R)/(B-R), S = 1.0-R/B, P = P)
      } else {
        HSP(H = 4.0/6.0 + 1.0/6.0*(R-G)/(B-G), S = 1.0-G/B, P = P)
      }
    }
  }

  // public domain function by Darel Rex Finley, 2006
  // adapted to Scala by Hanns Holger Rutz, 2018
  //
  // This function expects the passed-in values to be on a scale
  // of 0 to 1, and uses that same scale for the return values.
  //
  // Note that some combinations of HSP, even if in the scale
  // 0-1, may return RGB values that exceed a value of 1.  For
  // example, if you pass in the HSP color 0,1,1, the result
  // will be the RGB color 2.037,0,0.
  //
  // See description/examples at alienryderflex.com/hsp.html

  def HSPtoRGB(hsp: HSP): RGB = {
    import hsp._

    val minOverMax = 1.0 - S

    if (minOverMax > 0.0) {
      if (H < 1.0/6.0) {        //  R>G>B
        val Ht    = 6.0*(H - 0.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val B     = P / math.sqrt(Pr/minOverMax/minOverMax + Pg*part*part + Pb)
        val R     = B / minOverMax
        val G     = B + Ht*(R - B)
        RGB(R = R, G = G, B = B)
      } else if (H < 2.0/6.0) {   //  G>R>B
        val Ht    = 6.0*(-H + 2.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val B     = P / math.sqrt(Pg/minOverMax/minOverMax + Pr*part*part + Pb)
        val G     = B / minOverMax
        val R     = B + Ht*(G - B)
        RGB(R = R, G = G, B = B)
      } else if (H < 3.0/6.0) {   //  G>B>R
        val Ht    = 6.0*(H - 2.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val R     = P / math.sqrt(Pg/minOverMax/minOverMax + Pb*part*part + Pr)
        val G     = R / minOverMax
        val B     = R + Ht*(G - R)
        RGB(R = R, G = G, B = B)
      } else if ( H < 4.0/6.0) {  //  B>G>R
        val Ht    = 6.0*(-H + 4.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val R     = P / math.sqrt(Pb/minOverMax/minOverMax + Pg*part*part + Pr)
        val B     = R / minOverMax
        val G     = R + Ht*(B - R)
        RGB(R = R, G = G, B = B)
      } else if (H < 5.0/6.0) {   //  B>R>G
        val Ht    = 6.0*(H - 4.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val G     = P / math.sqrt(Pb/minOverMax/minOverMax + Pr*part*part + Pg)
        val B     = G / minOverMax
        val R     = G + Ht*(B - G)
        RGB(R = R, G = G, B = B)
      } else {                  //  R>B>G
        val Ht    = 6.0*(-H + 6.0/6.0)
        val part  = 1.0 + Ht*(1.0/minOverMax - 1.0)
        val G     = P / math.sqrt(Pr/minOverMax/minOverMax + Pb*part*part + Pg)
        val R     = G / minOverMax
        val B     = G + Ht*(R - G)
        RGB(R = R, G = G, B = B)
      }
    } else {
      if (H < 1.0/6.0) {   //  R>G>B
        val Ht  = 6.0*(H-0.0/6.0)
        val R   = math.sqrt(P*P/(Pr+Pg*Ht*Ht))
        val G   = R *Ht
        val B   = 0.0
        RGB(R = R, G = G, B = B)
      } else if (H < 2.0/6.0) {   //  G>R>B
        val Ht  = 6.0*(-H+2.0/6.0)
        val G   = math.sqrt(P*P/(Pg+Pr*Ht*Ht))
        val R   = G *Ht
        val B   = 0.0
        RGB(R = R, G = G, B = B)
      } else if (H < 3.0/6.0) {   //  G>B>R
        val Ht  = 6.0*(H-2.0/6.0)
        val G   = math.sqrt(P*P/(Pg+Pb*Ht*Ht))
        val B   = G *Ht
        val R   = 0.0
        RGB(R = R, G = G, B = B)
      } else if (H < 4.0/6.0) {   //  B>G>R
        val Ht  = 6.0*(-H+4.0/6.0)
        val B   = math.sqrt(P*P/(Pb+Pg*Ht*Ht))
        val G   = B *Ht
        val R   = 0.0
        RGB(R = R, G = G, B = B)
      } else if (H < 5.0/6.0) {   //  B>R>G
        val Ht  = 6.0*(H-4.0/6.0)
        val B   = math.sqrt(P*P/(Pb+Pr*Ht*Ht))
        val R   = B *Ht
        val G   = 0.0
        RGB(R = R, G = G, B = B)
      } else               {   //  R>B>G
        val Ht  = 6.0*(-H+6.0/6.0)
        val R   = math.sqrt(P*P/(Pr+Pb*Ht*Ht))
        val B   = R *Ht
        val G   = 0.0
        RGB(R = R, G = G, B = B)
      }
    }
  }
}
