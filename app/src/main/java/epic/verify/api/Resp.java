package epic.verify.api;

import epic.verify.api.Json.Obj;

public class Resp {

    public int code;
    public String msg = "";
    public Obj data;
    public long time;
    public Obj layout;
    public Obj privateTemplate;
    public Obj publicTemplate;
    public String raw;

    public boolean isSuccess() {
        return code == 200;
    }

    @Override
    public String toString() {
        return raw == null ? "Resp{code=" + code + ",msg=" + msg + "}" : raw;
    }
}
