/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;
import com.tc.util.thirdparty.base64.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The standard implementation of {@link TerracottaLicense}.
 */
public class StandardTerracottaLicense implements TerracottaLicense {

  public static final String  HACKED_MESSAGE     = "So, you've hacked this. Boy, wasn't that hard? Congratulations. "
                                                   + "(Yes, we weren't trying to protect against people like you.)";

  private static final String CANONICAL_ENCODING = "UTF-8";

  /**
   * One of the items in the license. Knows how to write itself to a string, read itself from a string, and validate its
   * data.
   */
  private static abstract class ConfigItem {
    private static final String  KEY_SEPARATOR_INPUT  = ":";
    private static final String  KEY_SEPARATOR_OUTPUT = KEY_SEPARATOR_INPUT + " ";

    private static final Pattern INPUT_PATTERN        = Pattern.compile("^\\s*([^" + KEY_SEPARATOR_INPUT + "]+?)\\s*"
                                                                        + KEY_SEPARATOR_INPUT + "\\s*(\\S.*?)\\s*$");

    private final String         key;
    private Object               value;

    protected ConfigItem(String key) {
      Assert.assertNotBlank(key);
      this.key = key;
      this.value = null;
    }

    protected final String key() {
      return this.key;
    }

    protected final void setRawValue(Object o) {
      Assert.eval(!isValueSet());
      this.value = o;
      validate();
    }

    protected final Object getRawValue() {
      Assert.eval(isValueSet());
      return this.value;
    }

    public String writeToString() {
      return this.key + KEY_SEPARATOR_OUTPUT + valueAsString();
    }

    /**
     * @return <code>true</code> if and only if the string is, in fact, a specification of this config item
     * @throws ParseException if and only if the string is not a valid specification of any config item &mdash;
     *         <em>i.e.</em>, it doesn't follow the correct format for a config item
     */
    public boolean readFromString(String string) throws ParseException {
      Matcher matcher = INPUT_PATTERN.matcher(string);
      if (!matcher.matches()) throw new ParseException("Line is not a valid key-value pair: '" + string + "'.", -1);
      if (!matcher.group(1).equalsIgnoreCase(key)) return false;
      else {
        setRawValue(valueFromString(matcher.group(2)));
        return true;
      }
    }

    public final boolean isValueSet() {
      return this.value != null;
    }

    public final void validate() throws IllegalArgumentException {
      Assert.assertNotNull(this.value);
      doValidate(this.value);
    }

    protected abstract Object valueFromString(String theValue) throws ParseException;

    protected abstract String valueAsString();

    protected abstract void doValidate(Object o) throws IllegalArgumentException;

    public boolean equals(Object that) {
      if (!(getClass().isInstance(that))) return false;
      ConfigItem thatItem = (ConfigItem) that;
      return new EqualsBuilder().append(this.key, thatItem.key).append(this.value, thatItem.value).isEquals();
    }

    public int hashCode() {
      return new HashCodeBuilder().append(this.key).append(this.value).toHashCode();
    }

    public String toString() {
      return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("key", this.key).append("value",
                                                                                                       this.value)
          .toString();
    }
  }

  private static abstract class IntConfigItem extends ConfigItem {
    public IntConfigItem(String key) {
      super(key);
    }

    protected Object valueFromString(String value) throws ParseException {
      try {
        return new Integer(value);
      } catch (NumberFormatException nfe) {
        throw new ParseException("Invalid value for key '" + key() + "': '" + value + "' is not a valid integer.", -1);
      }
    }

    protected String valueAsString() {
      return getRawValue().toString();
    }

    public int value() {
      return ((Integer) super.getRawValue()).intValue();
    }

    public void setValue(int value) {
      setRawValue(new Integer(value));
    }
  }

  private static abstract class StringConfigItem extends ConfigItem {
    public StringConfigItem(String key) {
      super(key);
    }

    protected Object valueFromString(String value) {
      return value.trim();
    }

    protected String valueAsString() {
      return ((String) getRawValue()).trim();
    }

    public String value() {
      return (String) super.getRawValue();
    }

    public void setValue(String value) {
      setRawValue(value.trim());
    }
  }

  private static class StringArrayConfigItem extends ConfigItem {
    public StringArrayConfigItem(String key) {
      super(key);
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      if (!(o instanceof String[])) throw new IllegalArgumentException("" + o + " is not a valid boolean value.");
      String[] data = (String[]) o;
      for (int i = 0; i < data.length; ++i) {
        if (StringUtils.isBlank(data[i])) throw new IllegalArgumentException("Element " + i
                                                                             + " of this array is blank.");
      }
    }

    protected String valueAsString() {
      StringBuffer out = new StringBuffer();
      String[] data = (String[]) super.getRawValue();

      for (int i = 0; i < data.length; ++i) {
        if (i > 0) out.append(", ");
        out.append(data[i]);
      }

      return out.toString();
    }

    protected Object valueFromString(String theValue) throws ParseException {
      if (StringUtils.isBlank(theValue)) throw new ParseException("'" + theValue + "' is blank.", 0);

      String[] out = theValue.split("\\s*,\\s*");

      for (int i = 0; i < out.length; ++i) {
        out[i] = out[i].trim();
        if (StringUtils.isBlank(out[i])) throw new ParseException("Element " + i + " is blank.", 0);
      }

      return out;
    }

    public void setValue(String[] value) {
      setRawValue(value);
    }

    public String[] value() {
      if (!isValueSet()) throw new IllegalArgumentException("Value is not set");
      return (String[]) getRawValue();
    }
  }

  public static class BooleanConfigItem extends ConfigItem {
    public BooleanConfigItem(String key) {
      super(key);
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      if (!(o instanceof Boolean)) throw new IllegalArgumentException("" + o + " is not a valid boolean value.");
    }

    protected String valueAsString() {
      return super.getRawValue().toString();
    }

    private static final String[] YES_VALUES = new String[] { "yes", "y", "1", "on", "true", "t", "si", "oui" };
    private static final String[] NO_VALUES  = new String[] { "no", "n", "0", "off", "false", "f", "non" };

    protected Object valueFromString(String theValue) throws ParseException {
      theValue = StringUtils.trimToNull(theValue);

      for (int i = 0; i < YES_VALUES.length; ++i) {
        if (theValue.equalsIgnoreCase(YES_VALUES[i])) return Boolean.TRUE;
      }

      for (int i = 0; i < NO_VALUES.length; ++i) {
        if (theValue.equalsIgnoreCase(NO_VALUES[i])) return Boolean.FALSE;
      }

      throw new ParseException("'" + theValue + "' is not a valid boolean string.", 0);
    }

    public void setValue(boolean value) {
      setRawValue(new Boolean(value));
    }

    public boolean value() {
      if (!isValueSet()) throw new IllegalArgumentException("Value is not set");
      return ((Boolean) getRawValue()).booleanValue();
    }
  }

  private static class RoundDateConfigItem extends ConfigItem {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static {
      DATE_FORMAT.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
    }

    public RoundDateConfigItem(String key) {
      super(key);
    }

    protected Object valueFromString(String value) throws ParseException {
      return DATE_FORMAT.parse(value);
    }

    protected String valueAsString() {
      return DATE_FORMAT.format((Date) getRawValue());
    }

    public Date value() {
      return (Date) super.getRawValue();
    }

    public void setValue(Date value) {
      setRawValue(value);
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      cal.setTime((Date) o);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      if (!cal.getTime().equals(o)) throw new IllegalArgumentException(
                                                                       "Value for '"
                                                                           + key()
                                                                           + "' specifies a time that isn't at midnight GMT.");
    }
  }

  private static class PositiveIntConfigItem extends IntConfigItem {
    public PositiveIntConfigItem(String key) {
      super(key);
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      if (((Integer) o).intValue() <= 0) throw new IllegalArgumentException("Value for '" + key()
                                                                            + "' must be positive.");
    }
  }

  private static class NonBlankStringConfigItem extends StringConfigItem {
    public NonBlankStringConfigItem(String key) {
      super(key);
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      if (StringUtils.isBlank((String) o)) throw new IllegalArgumentException("No value specified for '" + key() + "'.");
    }
  }

  private static class EnumeratedStringConfigItem extends NonBlankStringConfigItem {
    private final String[] possibleValues;

    public EnumeratedStringConfigItem(String key, String[] possibleValues) {
      super(key);
      Assert.assertNoNullElements(possibleValues);
      this.possibleValues = possibleValues;
    }

    protected void doValidate(Object o) throws IllegalArgumentException {
      super.doValidate(o);
      for (int i = 0; i < this.possibleValues.length; ++i) {
        if (o.equals(possibleValues[i])) return;
      }
      throw new IllegalArgumentException("The value '" + o + "' is not any of the valid values: "
                                         + ArrayUtils.toString(this.possibleValues));
    }
  }

  private static class LineEndNormalizingInputStream extends InputStream {
    private final PushbackInputStream source;

    public LineEndNormalizingInputStream(InputStream source) {
      Assert.assertNotNull(source);
      this.source = new PushbackInputStream(source, 1);
    }

    public int read() throws IOException {
      int ch = this.source.read();

      if (ch == '\r') {
        int nextChar = this.source.read();
        if (nextChar != '\n') this.source.unread(nextChar);

        ch = '\n';
      }

      return ch;
    }
  }

  /**
   * Knows the outer structure of a license file: the header, data, signature separator, signature, and footer. Does
   * everything in bytes, so that we can make sure our signature stands intact.
   */
  private static class LicenseFile {
    private static final byte[] LICENSE_HEADER;
    private static final byte[] LICENSE_SIGNATURE;
    private static final byte[] LICENSE_FOOTER;

    static {
      try {
        LICENSE_HEADER = "--------------------- BEGIN TERRACOTTA LICENSE KEY ---------------------\n"
            .getBytes(CANONICAL_ENCODING);
        LICENSE_SIGNATURE = "---------------------------- BEGIN SIGNATURE ---------------------------\n"
            .getBytes(CANONICAL_ENCODING);
        LICENSE_FOOTER = "---------------------- END TERRACOTTA LICENSE KEY ----------------------\n"
            .getBytes(CANONICAL_ENCODING);
      } catch (UnsupportedEncodingException uee) {
        throw Assert.failure("This Java doesn't understand UTF-8? Huh?", uee);
      }
    }

    private final byte[]        data;
    private final byte[]        signature;

    public LicenseFile(InputStream in) throws IOException, ParseException {
      ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
      ByteArrayOutputStream signatureStream = new ByteArrayOutputStream();

      expectLine(in, LICENSE_HEADER);

      while (true) {
        byte[] line = readNonBlankLine(in);
        if (equals(line, LICENSE_SIGNATURE)) break;
        else dataStream.write(line);
      }

      this.data = dataStream.toByteArray();

      while (true) {
        byte[] line = readNonBlankLine(in);
        if (equals(line, LICENSE_FOOTER)) break;
        else signatureStream.write(line);
      }

      this.signature = signatureStream.toByteArray();
    }

    public LicenseFile(byte[] data, byte[] signature) {
      Assert.assertNotNull(data);
      Assert.assertNotNull(signature);

      this.data = new byte[data.length + 1];
      System.arraycopy(data, 0, this.data, 0, data.length);
      this.data[this.data.length - 1] = '\n';
      this.signature = new byte[signature.length + 1];
      System.arraycopy(signature, 0, this.signature, 0, signature.length);
      this.signature[this.signature.length - 1] = '\n';
    }

    public byte[] data() {
      return this.data;
    }

    public byte[] signature() {
      return this.signature;
    }

    public void writeTo(OutputStream out) throws IOException {
      out.write(LICENSE_HEADER);
      out.write(this.data);
      out.write(LICENSE_SIGNATURE);
      out.write(this.signature);
      out.write(LICENSE_FOOTER);
    }

    private void expectLine(InputStream in, byte[] expected) throws IOException, ParseException {
      byte[] line = readNonBlankLine(in);
      boolean equals = equals(expected, line);
      if (!equals) throw new ParseException(
                                            "Expecting '" + new String(expected) + "'; got '" + new String(line) + "'.",
                                            -1);
    }

    private boolean equals(byte[] one, byte[] two) {
      boolean equals = two.length == one.length;
      for (int i = 0; i < two.length; ++i)
        equals = equals && two[i] == one[i];
      return equals;
    }

    private byte[] readNonBlankLine(InputStream in) throws IOException, ParseException {
      byte[] out;

      do {
        out = readLine(in);
        if (out == null) throw new ParseException("Expected data, but found none.", -1);
      } while (isBlank(out));

      return out;
    }

    private boolean isBlank(byte[] theData) {
      boolean hasData = false;

      for (int i = 0; i < theData.length; ++i) {
        hasData = hasData || (!Character.isWhitespace((char) theData[i]));
      }

      return !hasData;
    }

    private byte[] readLine(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int ch;

      while (true) {
        ch = in.read();

        if (ch == -1) {
          if (out.size() == 0) return null;
          else break;
        }

        out.write(ch);
        if (ch == '\n') break;
      }

      return out.toByteArray();
    }
  }

  /**
   * A list of {@link ConfigItem}s. Knows how to read itself from a {@link BufferedReader} and write itself to a
   * {@link Writer}.
   */
  private static class ConfigItemList {
    private final ConfigItem[] configItems;

    public ConfigItemList(ConfigItem[] configItems) {
      Assert.assertNoNullElements(configItems);
      Assert.eval(configItems.length > 0);

      this.configItems = configItems;
    }

    public void writeTo(Writer out) throws IOException {
      for (int i = 0; i < configItems.length; ++i) {
        if (configItems[i].isValueSet()) {
          out.write(configItems[i].writeToString() + "\n");
        }
      }
    }

    public void readFrom(BufferedReader in) throws IOException, ParseException {
      String line;

      while ((line = in.readLine()) != null) {
        boolean found = false;

        if (line.trim().length() == 0) continue;
        for (int i = 0; i < configItems.length; ++i) {
          if (configItems[i].readFromString(line)) {
            found = true;
            break;
          }
        }

        if (!found) throw new ParseException("Unknown line: '" + line + "'.", -1);
      }
    }
  }

  public static final String             LICENSE_TYPE_TRIAL      = "trial";
  public static final String             LICENSE_TYPE_PRODUCTION = "production";

  private static final String[]          ALL_LICENSE_TYPES       = { LICENSE_TYPE_TRIAL, LICENSE_TYPE_PRODUCTION };

  private final PositiveIntConfigItem    serialNumber            = new PositiveIntConfigItem("Serial Number");
  private final NonBlankStringConfigItem licensee                = new NonBlankStringConfigItem("Licensee");
  private final NonBlankStringConfigItem licenseType             = new EnumeratedStringConfigItem("License Type",
                                                                                                  ALL_LICENSE_TYPES);
  private final RoundDateConfigItem      l2ExpiresOn             = new RoundDateConfigItem("L2 Expires");
  private final PositiveIntConfigItem    maxL2Connections        = new PositiveIntConfigItem("Maximum L1s Per L2");
  private final PositiveIntConfigItem    maxL2RuntimeSeconds     = new PositiveIntConfigItem(
                                                                                             "Maximum L2 Uptime (seconds)");
  private final StringArrayConfigItem    enabledModules          = new StringArrayConfigItem("Enabled modules");

  private final BooleanConfigItem        dsoHAEnabled            = new BooleanConfigItem("DSO HA Enabled");

  private final ConfigItemList           itemList                = new ConfigItemList(new ConfigItem[] {
      this.serialNumber, this.licensee, this.licenseType, this.l2ExpiresOn, this.maxL2Connections,
      this.maxL2RuntimeSeconds, this.enabledModules, this.dsoHAEnabled });

  public StandardTerracottaLicense(InputStream data, Signature signature, PublicKey publicKey) throws IOException,
      ParseException, SignatureException, InvalidKeyException {
    Assert.assertNotNull(data);
    Assert.assertNotNull(signature);
    Assert.assertNotNull(publicKey);

    LineEndNormalizingInputStream stream = new LineEndNormalizingInputStream(data);
    LicenseFile file = new LicenseFile(stream);

    signature.initVerify(publicKey);
    signature.update(file.data());
    if (!signature.verify(Base64.decode(new String(file.signature(), CANONICAL_ENCODING)))) {
      // formatting
      throw new SignatureException("Signature on this license file is not valid: the file is corrupt or has been "
                                   + "modified.");
    }

    itemList.readFrom(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file.data()))));
  }

  public StandardTerracottaLicense(int serialNumber, String licensee, String licenseType, Date l2ExpiresOn,
                                   int maxL2Connections, int maxL2RuntimeSeconds, String[] enabledModules,
                                   boolean dsoHAEnabled) {
    Assert.eval(serialNumber > 0);
    Assert.assertNotBlank(licensee);
    Assert.assertNotBlank(licenseType);
    Assert.eval(maxL2Connections > 0);
    Assert.eval(maxL2RuntimeSeconds >= 10);
    Assert.assertNoNullElements(enabledModules);

    this.serialNumber.setValue(serialNumber);
    this.licensee.setValue(licensee);
    this.licenseType.setValue(licenseType);
    if (l2ExpiresOn != null) this.l2ExpiresOn.setValue(l2ExpiresOn);
    if (maxL2Connections < Integer.MAX_VALUE) this.maxL2Connections.setValue(maxL2Connections);
    if (maxL2RuntimeSeconds < Integer.MAX_VALUE) this.maxL2RuntimeSeconds.setValue(maxL2RuntimeSeconds);
    this.enabledModules.setValue(enabledModules);
    if (dsoHAEnabled) this.dsoHAEnabled.setValue(dsoHAEnabled);
  }

  public void writeTo(OutputStream output, Signature signature, PrivateKey privateKey) throws IOException,
      SignatureException, InvalidKeyException {
    StringWriter stringWriter = new StringWriter();
    itemList.writeTo(stringWriter);
    byte[] data = stringWriter.toString().getBytes(CANONICAL_ENCODING);

    signature.initSign(privateKey);
    signature.update(data);
    byte[] signatureData = signature.sign();

    LicenseFile file = new LicenseFile(data, Base64.encodeBytes(signatureData).getBytes(CANONICAL_ENCODING));
    file.writeTo(output);
  }

  public Date l2ExpiresOn() {
    return this.l2ExpiresOn.isValueSet() ? this.l2ExpiresOn.value() : null;
  }

  public String licensee() {
    return this.licensee.value();
  }

  public int maxL2Connections() {
    return this.maxL2Connections.isValueSet() ? this.maxL2Connections.value() : Integer.MAX_VALUE;
  }

  public long maxL2RuntimeMillis() {
    return this.maxL2RuntimeSeconds.isValueSet() ? this.maxL2RuntimeSeconds.value() * 1000L : Long.MAX_VALUE;
  }

  public int serialNumber() {
    return this.serialNumber.value();
  }

  public String licenseType() {
    return this.licenseType.value();
  }

  public boolean dsoHAEnabled() {
    return this.dsoHAEnabled.isValueSet();
  }

  private static final String MODULE_ALL = "All";

  public boolean isModuleEnabled(String moduleName) {
    Assert.assertNotBlank(moduleName);
    moduleName = moduleName.trim();

    String[] enabled = this.enabledModules.value();
    for (int i = 0; i < enabled.length; ++i) {
      if (enabled[i].trim().equalsIgnoreCase(moduleName) || enabled[i].trim().equalsIgnoreCase(MODULE_ALL)) return true;
    }

    return false;
  }

  public int hashCode() {
    return new HashCodeBuilder().append(this.serialNumber).append(this.licensee).append(this.licenseType)
        .append(this.l2ExpiresOn).append(this.maxL2Connections).append(this.maxL2RuntimeSeconds)
        .append(toSet(this.enabledModules.value())).toHashCode();
  }

  private Set toSet(Object[] data) {
    Set out = new HashSet();
    out.addAll(Arrays.asList(data));
    return out;
  }

  public boolean equals(Object that) {
    if (!(that instanceof StandardTerracottaLicense)) return false;
    StandardTerracottaLicense thatLicense = (StandardTerracottaLicense) that;

    return new EqualsBuilder().append(this.serialNumber, thatLicense.serialNumber).append(this.licensee,
                                                                                          thatLicense.licensee)
        .append(this.licenseType, thatLicense.licenseType).append(this.l2ExpiresOn, thatLicense.l2ExpiresOn)
        .append(this.maxL2Connections, thatLicense.maxL2Connections).append(this.maxL2RuntimeSeconds,
                                                                            thatLicense.maxL2RuntimeSeconds)
        .append(toSet(this.enabledModules.value()), toSet(thatLicense.enabledModules.value())).isEquals();
  }

  public String toString() {
    return new ToStringBuilder(this).append("serial number", this.serialNumber).append("licensee", this.licensee)
        .append("license type", this.licenseType).append("server expires on", this.l2ExpiresOn)
        .append("max server connections", this.maxL2Connections).append("max server runtime millis",
                                                                        this.maxL2RuntimeSeconds)
        .append("enabled modules", this.enabledModules).toString();
  }

  private String millisToTime(long millis) {
    long hours = (millis / (60L * 60L * 1000L));
    long minutes = (millis / (60L * 1000L));
    long seconds = (millis / 1000L);

    StringBuffer out = new StringBuffer();
    if (hours > 0) out.append(pluralize(hours, "hour"));
    if (minutes > 0) {
      if (hours > 0) out.append(", ");
      out.append(pluralize(minutes, "minute"));
    }
    if (seconds > 0) {
      if (hours > 0 || minutes > 0) out.append(", ");
      out.append(pluralize(seconds, "second"));
    }

    return out.toString();
  }

  private String pluralize(long howMany, String word) {
    if (howMany == 1) return "1 " + word;
    else return howMany + " " + word + "s";
  }

  public String describe() {
    String out = "#" + serialNumber() + ", for '" + licensee() + "', type: " + licenseType();
    if (l2ExpiresOn() != null) out += "; expires: " + l2ExpiresOn();
    if (maxL2Connections() < Integer.MAX_VALUE) out += "; max clients: " + maxL2Connections();
    if (maxL2RuntimeMillis() < Long.MAX_VALUE) out += "; max server runtime: " + millisToTime(maxL2RuntimeMillis());
    out += "; enabled modules: " + ArrayUtils.toString(this.enabledModules.value());
    out += "; DSO HA enabled: " + dsoHAEnabled();
    return out;
  }

}
