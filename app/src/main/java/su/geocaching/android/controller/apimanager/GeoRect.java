package su.geocaching.android.controller.apimanager;


import com.google.android.gms.maps.model.LatLng;

public class GeoRect {

    public LatLng tl;
    public LatLng br;

    public GeoRect(LatLng tl, LatLng br) {
        assert (tl.latitude < br.latitude || tl.longitude == br.longitude);
        this.tl = tl;
        this.br = br;
    }

    public boolean contains(GeoRect rect) {
        if (rect.tl.latitude > tl.latitude) return false;
        if (rect.br.latitude < br.latitude) return false;

        if (br.longitude > tl.longitude) {
            if (rect.br.longitude > rect.tl.longitude) {
                if (rect.tl.longitude < tl.longitude || rect.br.longitude > br.longitude)
                    return false;
            } else {
                return false;
            }
        } else {
            if (rect.br.longitude > rect.tl.longitude) {
                if (rect.tl.longitude > tl.latitude) return true;
                if (rect.br.longitude < br.longitude) return true;
            } else {
                if ((rect.tl.longitude < tl.longitude && rect.tl.longitude > br.longitude)
                        || ((rect.br.longitude < tl.longitude && rect.br.longitude > br.longitude)))
                    return false;
            }
        }
        return true;
    }

    public boolean contains(LatLng point) {
        if (point.latitude > tl.latitude) return false;
        if (point.latitude < br.latitude) return false;

        if (br.longitude > tl.longitude) {
            if (point.longitude < tl.longitude || point.longitude > br.longitude) return false;
        } else {
            // rightLong maybe smaller than leftLong. 4ex 160:-160
            if (point.longitude < tl.longitude && point.longitude > br.longitude) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", tl, br);
    }

}
