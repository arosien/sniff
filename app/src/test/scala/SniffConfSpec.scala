package net.rosien.sniff

import org.specs2.Specification

class SniffConfSpec extends Specification {
  def is = "app conf should" ^
      "not accept invalid languages" ! noInvalidLangs ^
      end
      
  def noInvalidLangs = {
//    new App.SniffConf(Array("--lang", "foo")).lang.orElse(None).get must beNone
    pending
  }
}