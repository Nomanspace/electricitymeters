package org.nomanspace.electricitymeters.text;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.util.LogUtil;

public class DatFileParseHandlerTest {

    @BeforeAll
    static void init() {
        LogUtil.setLoggingEnabled(true);
    }


    @Test
    void testForUnderstandingHowParseWork() {
        DatFileParseHandler handler = new DatFileParseHandler();
        List<String> input = List.of(
                "\tTYPE=PLC_I_CONCENTRATOR; ADDR=2001",
                "\t\tTYPE=PLC_I_METER; ADDR=7; HOST=2001; TIMEDATE=1D00120719; BINDATA=8507004FA200004D041D001207194F4D070025043A171E06194F8E060045043B171D0519"
        );
        List<Concentrator> result = handler.process(input);
        System.out.println(result);
    }

}
