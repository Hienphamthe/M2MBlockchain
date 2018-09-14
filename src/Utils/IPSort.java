package Utils;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 *
 * @author Student
 */
public class IPSort {

    public static class InetAddressComparator implements Comparator<InetAddress> {
        @Override
        public int compare(InetAddress a, InetAddress b) {
            byte[] aOctets = a.getAddress(),
                   bOctets = b.getAddress();
            int len = Math.max(aOctets.length, bOctets.length);
            for (int i = 0; i < len; i++) {
                byte aOctet = (i >= len - aOctets.length) ?
                    aOctets[i - (len - aOctets.length)] : 0;
                byte bOctet = (i >= len - bOctets.length) ?
                    bOctets[i - (len - bOctets.length)] : 0;
                if (aOctet != bOctet) return (0xff & aOctet) - (0xff & bOctet);
            }
            return 0;
        }
    }

    public static Optional<InetAddress> toInetAddress(String s) {
        try {
            return Optional.of(InetAddress.getByName(s));
        } catch (UnknownHostException badAddress) {
            return Optional.empty();
        }
    }

    public static List<String> IPSort(List<String> data) throws Exception {
        return data.stream()
              .map(IPSort::toInetAddress)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .sorted(new InetAddressComparator())
              .map(InetAddress::getHostAddress)
              .collect(Collectors.toList());
    }
}
