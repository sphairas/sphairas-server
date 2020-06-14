<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:fo="http://www.w3.org/1999/XSL/Format" 
                xmlns:svg="http://www.w3.org/2000/svg">
    
    <xsl:param name="background-image" />
    
    <xsl:template match="/">
        <fo:root>
            <fo:layout-master-set>
                <fo:simple-page-master master-name="page.first" 
                                       page-width="21.0cm" 
                                       page-height="29.7cm" 
                                       margin-top="1.5cm" 
                                       margin-bottom="1.5cm" 
                                       margin-left="2.0cm" 
                                       margin-right="2.0cm">
                    <fo:region-body region-name="body" background-position-horizontal="center" background-position-vertical="center" >
                        <xsl:if test="$background-image" >
                            <xsl:attribute name="background-image">
                                <xsl:value-of select="$background-image" />
                            </xsl:attribute>                            
                        </xsl:if>
                    </fo:region-body>
                </fo:simple-page-master>
                <fo:simple-page-master master-name="page" page-width="21.0cm" page-height="29.7cm" margin-top="1.5cm" margin-bottom="1.5cm" margin-left="2.0cm" margin-right="2.0cm">
                    <fo:region-body region-name="body" /> 
                </fo:simple-page-master>
                <fo:page-sequence-master master-name="master">                     ❹
                    <fo:repeatable-page-master-alternatives>
                        <fo:conditional-page-master-reference master-reference="page.first" page-position="first" />
                        <fo:conditional-page-master-reference master-reference="page" />
                    </fo:repeatable-page-master-alternatives>
                </fo:page-sequence-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="master" language="de">
                <fo:flow flow-name="body">
                    <fo:block font-size="12pt" font-family="SansSerif">
                        Keine Vorlage.
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
    
</xsl:stylesheet>
