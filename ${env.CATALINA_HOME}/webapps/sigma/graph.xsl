<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:transform  version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="Ups|Downs">
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="Up">
<xsl:value-of select="../../@NAME"/>-<xsl:value-of select="@NAME"/>,
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="Down">
        <xsl:value-of select="../../@NAME"/>=<xsl:value-of select="@NAME"/>,
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="Center">
        <xsl:apply-templates />
</xsl:template>

</xsl:transform>