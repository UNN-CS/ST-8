{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    # Java and build tool
    jdk17
    maven

    # Browsers and drivers
    chromium
    chromedriver

    # Optional: for debugging if needed
    xorg.libX11
    xorg.libXcomposite
    xorg.libXdamage
    xorg.libXext
    xorg.libXfixes
    xorg.libXrandr
    xorg.libxcb
    alsa-lib
    at-spi2-atk
    at-spi2-core
    cups
    dbus
    expat
    gdk-pixbuf
    glib
    gtk3
    libdrm
    libxkbcommon
    mesa
    nspr
    nss
    pango
    systemd
  ];

  shellHook = ''
    # Locate binaries
    CHROME_BIN="${pkgs.chromium}/bin/chromium"
    CHROMEDRIVER_BIN="${pkgs.chromedriver}/bin/chromedriver"

    # Set environment variables for Selenium
    export MAVEN_OPTS="-Dwebdriver.chrome.driver=$CHROMEDRIVER_BIN -Dwebdriver.chrome.bin=$CHROME_BIN"

    # Optional: run headless by default (code already uses headless=true)
    # To see the browser, set headless=false: mvn exec:java -Dheadless=false
    echo "Development environment ready for ST-8 Paper CD Case project"
    echo "Chrome (Chromium) : $CHROME_BIN"
    echo "ChromeDriver       : $CHROMEDRIVER_BIN"
    echo ""
    echo "To run the application:"
    echo "  mvn clean compile exec:java"
    echo ""
    echo "To see the browser window (headless=false):"
    echo "  mvn exec:java -Dheadless=false"
  '';
}
