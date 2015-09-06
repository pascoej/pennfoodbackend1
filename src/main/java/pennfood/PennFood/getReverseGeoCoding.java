package pennfood.PennFood;

import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

public class getReverseGeoCoding {
    private String Address1 = "", Address2 = "", City = "", State = "", Country = "", County = "", PIN = "";

    public void getAddress(String longi, String lati) {
        Address1 = "";
        Address2 = "";
        City = "";
        State = "";
        Country = "";
        County = "";
        PIN = "";

        try {

            JSONObject jsonObj = new Resty().json("http://geocoder.ca/?latt="+lati+"&longt="+longi+"&reverse=1&allna=1&geoit=xml&corner=1&json=1").object();
            System.out.println(jsonObj);
            PIN = jsonObj.getString("postal");
            City = jsonObj.getString("city");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getAddress1() {
        return Address1;

    }

    public String getAddress2() {
        return Address2;

    }

    public String getCity() {
        return City;

    }

    public String getState() {
        return State;

    }

    public String getCountry() {
        return Country;

    }

    public String getCounty() {
        return County;

    }

    public String getPIN() {
        return PIN;

    }

}