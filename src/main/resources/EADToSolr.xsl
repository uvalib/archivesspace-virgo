<?xml version="1.0" encoding="UTF-8"?>
<!--
   - An XSLT that takes an EAD file from ArchivesSpace and builds
   - SOLR index documents for the collection and each component 
   - within it.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:s="http://www.w3.org/2001/sw/DataAccess/rf1/result" xmlns:ead="urn:isbn:1-931666-22-9"
    xmlns:mods="http://www.loc.gov/mods/v3" xmlns:uva="http://indexing.virginia.edu/ead-extensions"
    exclude-result-prefixes="xs s" version="2.0">

    <xsl:output byte-order-mark="no" encoding="UTF-8" media-type="text/xml" xml:space="preserve" indent="yes"/>

    <xsl:param name="debug" required="no"/>
    <xsl:param name="released-facet" required="no"/>
    <xsl:param name="output" required="yes"/>

    <!-- Global Variables -->
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz    '"/>
    <!-- whitespace in select is meaningful -->
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ,;-:.'"/>
    <xsl:variable name="newline">
        <xsl:text>
</xsl:text>
    </xsl:variable>

    <xsl:template match="/">
        <xsl:variable name="collectionId"
            select="translate(/ead:ead/ead:archdesc/ead:did/ead:unitid, ' ', '')"/>
        <xsl:variable name="creationDate"
            select="/ead:ead/ead:eadheader/ead:profiledesc/ead:creation/ead:date/text()"/>
        <xsl:variable name="dateReceived"
            select="concat(substring($creationDate, 1, 4), substring($creationDate, 6, 2), substring($creationDate, 9, 2))"/>

        <!-- collection document -->
        <xsl:result-document href="{$output}/{$collectionId}.xml">
            <add>
                <doc>
                    <field name="id">
                        <xsl:value-of select="$collectionId"/>
                    </field>
                    <field name="source_facet">UVA Library Digital Repository</field>
                    <field name="feature_facet">suppress_ris_export</field>
                    <field name="feature_facet">suppress_refworks_export</field>
                    <field name="feature_facet">suppress_endnote_export</field>
                    <xsl:if test="$released-facet">
                        <field name="released_facet">
                            <xsl:value-of select="$released-facet"/>
                        </field>
                    </xsl:if>
                    <field name="shadowed_location_facet">VISIBLE</field>
                    <field name="date_received_facet">
                        <xsl:value-of select="$dateReceived"/>
                    </field>

                    <xsl:apply-templates select="*" mode="collection"/>

                    <!-- Add the ead fragment -->
                    <field name="feature_facet">has_ead_Fragment</field>
                    <field name="raw_ead_display">
                        <xsl:value-of select="'&lt;![CDATA['" disable-output-escaping="yes"/>
                        <xsl:apply-templates select="." mode="raw"/>
                        <xsl:value-of select="']]&gt;'" disable-output-escaping="yes"/>
                    </field>

                    <!-- add the hierarchy -->
                    <field name="feature_facet">has_hierarchy</field>
                    <field name="hierarchy_display">
                        <xsl:value-of select="'&lt;![CDATA['" disable-output-escaping="yes"/>
                        <xsl:apply-templates select="*" mode="hierarchy"/>
                        <xsl:value-of select="']]&gt;'" disable-output-escaping="yes"/>
                    </field>

                    <!-- Breadcrumbs aren't needed at the collection level. -->
                </doc>
            </add>
        </xsl:result-document>

        <!-- Build citation display -->
        <!--
              <xsl:if test="$ancestors/ancestor">
                <field name="feature_facet">has_citation</field>
                <field name="modified_chicago_citation_display">
                  <xsl:value-of select="//did/unittitle/text()[normalize-space(.)]" />
                  <xsl:text>, </xsl:text>
                  <xsl:value-of select="//unitdate/text()[normalize-space(.)]" />
                  <xsl:text>, </xsl:text>
                  <xsl:for-each select="//container[1]">
                      <xsl:value-of select="@label" /><xsl:text> </xsl:text><xsl:value-of select="text()" />  
                  </xsl:for-each>
                  <xsl:text>, </xsl:text>
                  <xsl:value-of select="normalize-space($ancestors/ancestor[1]/xmlcontent/ead/eadheader[1]/filedesc[1]/titlestmt[1]/titleproper[1])" />
                  <xsl:text>, </xsl:text>
                  <xsl:value-of select="normalize-space($ancestors/ancestor[1]/xmlcontent/ead/archdesc[1]/did[1]/repository[1])" />
                </field>
              </xsl:if>

                -->
        <!-- Add the container information -->
        <!-- 
                <xsl:variable name="container">
                    <xsl:call-template name="index-containers">
                        <xsl:with-param name="pid" select="$pid" />
                        <xsl:with-param name="ancestry" select="$ancestors" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:if test="string-length($container) &gt; 0">
                    <field name="feature_facet">has_archival_holdings</field>
                    <field name="container_display">
                        <xsl:copy-of select="$container" />
                    </field>
                </xsl:if>
                -->
    </xsl:template>

    <xsl:template match="*" priority="-1" mode="collection">
        <xsl:apply-templates select="*" mode="collection"/>
    </xsl:template>
    <xsl:template match="*" priority="-1" mode="component">
        <xsl:apply-templates select="*" mode="component"/>
    </xsl:template>
    <xsl:template match="*" priority="-1" mode="context">
        <xsl:apply-templates select="*" mode="context"/>
    </xsl:template>

    <xsl:template match="ead:ead/ead:archdesc/ead:did/ead:unitid" mode="collection">
        <field name="call_number_facet">
            <xsl:value-of select="normalize-space(text())"/>
        </field>
        <field name="call_number_display">
            <xsl:value-of select="normalize-space(text())"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:origination/*[@role = 'creator']" mode="collection context">
        <xsl:variable name="creator">
            <xsl:value-of select="text()"/>
        </xsl:variable>
        <field name="creator_display">
            <xsl:value-of select="$creator"/>
        </field>
        <field name="creator_text">
            <xsl:value-of select="$creator"/>
        </field>
        <field name="author_facet">
            <xsl:value-of select="$creator"/>
        </field>
        <field name="author_text">
            <xsl:value-of select="$creator"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:unitdate[1]" mode="collection component">
        <field name="date_display">
            <xsl:value-of select="text()"/>
        </field>
        <xsl:choose>
            <xsl:when test="@normal">
                <xsl:variable name="year" select="substring(@normal, 1, 4)"/>
                <field name="year_multisort_i">
                    <xsl:value-of select="$year"/>
                </field>
                <xsl:variable name="age"
                    select="number(substring(string(current-date()), 1, 4)) - number($year)"/>
                <xsl:if test="$age &lt;= 1">
                    <field name="published_date_facet">
                        <xsl:text>This year</xsl:text>
                    </field>
                </xsl:if>
                <xsl:if test="$age &lt;= 3">
                    <field name="published_date_facet">
                        <xsl:text>Last 3 years</xsl:text>
                    </field>
                </xsl:if>
                <xsl:if test="$age &lt;= 10">
                    <field name="published_date_facet">
                        <xsl:text>Last 10 years</xsl:text>
                    </field>
                </xsl:if>
                <xsl:if test="$age &lt;= 50">
                    <field name="published_date_facet">
                        <xsl:text>Last 50 years</xsl:text>
                    </field>
                </xsl:if>
                <xsl:if test="$age &gt; 50">
                    <field name="published_date_facet">
                        <xsl:text>More than 50 years ago</xsl:text>
                    </field>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <field name="published_date_facet">
                    <xsl:text>More than 50 years ago</xsl:text>
                </field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="ead:ead/ead:archdesc/ead:did/ead:unittitle[1]" mode="collection">
        <xsl:variable name="title">
            <xsl:for-each select="current()//text()">
                <xsl:value-of select="current()"/>
            </xsl:for-each>
        </xsl:variable>
        <field name="main_title_display">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="title_display">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="title_text" boost="2.0">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="digital_collection_facet">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:ead/ead:archdesc/ead:did/ead:unittitle" mode="context">
        <xsl:variable name="title">
            <xsl:for-each select="current()//text()">
                <xsl:value-of select="current()"/>
            </xsl:for-each>
        </xsl:variable>
        <field name="collection_title_display">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="digital_collection_facet">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="collection_title_text" boost="0.25">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:filedesc/ead:publicationstmt/ead:publisher" mode="collection context">
        <field name="library_facet">
            <xsl:value-of select="normalize-space(text())"/>
        </field>
        <field name="location_facet">
            <xsl:value-of select="normalize-space(text())"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:scopecontent" mode="collection">
        <!-- This section will no longer be necessary after the changes made for
           the Reed collection are deployed in April 2015.  It is useful during
           the transition period becuase it allows the old code to display the
           updated records properly and doesn't interfere with the new code-->
        <field name="scope_content_display">
            <xsl:text>&lt;scopecontent&gt;</xsl:text>
            <xsl:for-each select="current()/p">
                <xsl:variable name="p">
                    <xsl:for-each select="current()//text()">
                        <xsl:text>&lt;![CDATA[</xsl:text>
                        <xsl:value-of select="current()"/>
                        <xsl:text>]]&gt;</xsl:text>
                    </xsl:for-each>
                </xsl:variable>
                <xsl:text>&lt;p&gt;</xsl:text>
                <xsl:value-of select="normalize-space($p)"/>
                <xsl:text>&lt;/p&gt;</xsl:text>
            </xsl:for-each>
            <xsl:text>&lt;/scopecontent&gt;</xsl:text>
        </field>
        <!-- end of section to remove after April 2015 -->

        <xsl:variable name="content">
            <xsl:for-each select="current()//text()">
                <xsl:value-of select="current()"/>
            </xsl:for-each>
        </xsl:variable>
        <field name="scopecontent_text">
            <xsl:value-of select="normalize-space($content)"/>
        </field>

        <field name="scopecontent_display">
            <xsl:for-each select="current()/p">
                <xsl:variable name="p">
                    <xsl:for-each select="current()//text()">
                        <xsl:value-of select="current()"/>
                    </xsl:for-each>
                </xsl:variable>
                <xsl:text>&lt;p&gt;</xsl:text>
                <xsl:value-of select="normalize-space($p)"/>
                <xsl:text>&lt;/p&gt;</xsl:text>
            </xsl:for-each>
        </field>
    </xsl:template>

    <xsl:template match="ead:abstract" mode="collection component">
        <xsl:variable name="abstract">
            <xsl:for-each select="current()//text()">
                <xsl:value-of select="current()"/>
            </xsl:for-each>
        </xsl:variable>
        <field name="collection_abstract_text" boost="0.5">
            <xsl:value-of select="normalize-space($abstract)"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:did/ead:physdesc[1]/ead:extent[1]" mode="collection">
        <field name="extent_display">
            <xsl:value-of select="text()"/>
        </field>
        <field name="extent_text">
            <xsl:value-of select="text()"/>
        </field>

    </xsl:template>

    <xsl:template match="ead:did/ead:langmaterial/ead:language" mode="collection">
        <field name="language_facet">
            <xsl:value-of select="text()"/>
        </field>
        <field name="language_display">
            <xsl:value-of select="text()"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:controlaccess/ead:genreform" mode="collection">
        <field name="format_display">
            <xsl:value-of select="text()"/>
        </field>
        <field name="format_text">
            <xsl:value-of select="text()"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:container" mode="collection">
        <field name="location_display">
            <xsl:value-of select="@label"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="text()"/>
        </field>
        <field name="location_text">
            <xsl:value-of select="@label"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="text()"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:userestrict" mode="collection">
        <field name="access_display">
            <xsl:value-of select="ead:head"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="ead:p"/>
        </field>
        <field name="access_text">
            <xsl:value-of select="ead:head"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="ead:p"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:accessrestrict" mode="collection">
        <field name="access_display">
            <xsl:value-of select="ead:head"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="ead:p"/>
        </field>
        <field name="access_text">
            <xsl:value-of select="ead:head"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="ead:p"/>
        </field>
    </xsl:template>

    <xsl:template match="ead:unittitle" mode="collection component">
        <xsl:variable name="title">
            <xsl:for-each select="current()//text()">
                <xsl:value-of select="current()"/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:if test="not(current()/preceding-sibling::unittitle)">
            <field name="main_title_display">
                <xsl:value-of select="normalize-space($title)"/>
            </field>
        </xsl:if>
        <field name="title_display">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="title_text">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
        <field name="full_title_text">
            <xsl:value-of select="normalize-space($title)"/>
        </field>
    </xsl:template>

    <xsl:template match="uva:mods-metadata" mode="component">
        <xsl:call-template name="addFieldsFromMods">
            <xsl:with-param name="mods" select="mods:mods"/>
            <xsl:with-param name="useRightsString" select="'In Copyright'"/>
        </xsl:call-template>
        <!-- This is a rare, rich, item-level description -->
    </xsl:template>

    <xsl:template match="uva:thumbnail" mode="component">
        <field name="format_facet">Online</field>
        <field name="thumbnail_url_display">
            <xsl:value-of select="."/>
        </field>
    </xsl:template>

    <xsl:template match="uva:iiif-manifest" mode="component">
        <field name="feature_facet">iiif</field>
        <field name="iiif_presentation_metadata_display">
            <xsl:value-of select="text()"/>
        </field>

        <field name="feature_facet">rights_wrapper</field>
        <field name="rights_wrapper_url_display"
                >http://rightswrapper2.lib.virginia.edu:8090/rights-wrapper/?pid=<xsl:value-of
                select="../uva:pid"/>&amp;pagePid=</field>
        <field name="rights_wrapper_display"/>

        <field name="feature_facet">pdf_service</field>
        <field name="pdf_url_display">http://pdfws-test.lib.virginia.edu:8088/</field>

    </xsl:template>


    <xsl:template match="ead:ead" mode="collection">
        <xsl:if test="@id">
            <field name="ead_id_text" boost="2.0">
                <xsl:value-of select="@id"/>
            </field>
        </xsl:if>
        <field name="hierarchy_level_display">collection</field>
        <field name="feature_facet">display_ead_fragment</field>
        <xsl:if test="$debug">
            <xsl:comment>Matched EAD</xsl:comment>
        </xsl:if>
        <xsl:apply-templates select="*" mode="collection"/>
    </xsl:template>

    <xsl:template match="ead:c | node()[starts-with(name(), 'c0')]" mode="context"/>

    <xsl:template match="ead:c | node()[starts-with(name(), 'c0')]" mode="collection component">
        <xsl:if test="$debug">
            <xsl:comment>Matched <xsl:value-of select="name()"/></xsl:comment>
        </xsl:if>
        <xsl:variable name="id">
            <xsl:call-template name="getId">
                <xsl:with-param name="component" select="."/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:result-document href="{$output}/{$id}.xml">
            <add>
                <doc>
                    <field name="id">
                        <xsl:value-of select="$id"/>
                    </field>
                    <field name="hierarchy_level_display">
                        <xsl:value-of select="@level"/>
                    </field>
                    <field name="source_facet">UVA Library Digital Repository</field>
                    <field name="feature_facet">suppress_ris_export</field>
                    <field name="feature_facet">suppress_refworks_export</field>
                    <field name="feature_facet">suppress_endnote_export</field>
                    <xsl:if test="$released-facet">
                        <field name="released_facet">
                            <xsl:value-of select="$released-facet"/>
                        </field>
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="uva:mods-metadata">
                            <field name="shadowed_location_facet">VISIBLE</field>
                        </xsl:when>
                        <xsl:otherwise>
                            <field name="shadowed_location_facet">UNDISCOVERABLE</field>
                        </xsl:otherwise>
                    </xsl:choose>

                    <!--TODO : add this, though it doesn't really matter<field name="date_received_facet"><xsl:value-of select="$dateReceived" /></field> -->

                    <xsl:apply-templates select="*" mode="component"/>

                    <xsl:apply-templates select="/*" mode="context"/>

                    <!-- Add the ead fragment -->
                    <field name="feature_facet">has_ead_Fragment</field>
                    <field name="raw_ead_display">
                        <xsl:value-of select="'&lt;![CDATA['" disable-output-escaping="yes"/>
                        <xsl:apply-templates select="." mode="raw"/>
                        <xsl:value-of select="']]&gt;'" disable-output-escaping="yes"/>
                    </field>
                    <xsl:if test="not(@level eq 'item' or @level eq 'file')">
                        <field name="feature_facet">display_ead_fragment</field>
                    </xsl:if>

                    <!-- add the hierarchy -->
                    <field name="feature_facet">has_hierarchy</field>
                    <field name="hierarchy_display">
                        <xsl:value-of select="'&lt;![CDATA['" disable-output-escaping="yes"/>
                        <xsl:apply-templates select="." mode="hierarchy"/>
                        <xsl:value-of select="']]&gt;'" disable-output-escaping="yes"/>
                    </field>

                    <!-- add the breadcrumbs -->
                    <field name="breadcrumbs_display">
                        <xsl:value-of select="'&lt;![CDATA['" disable-output-escaping="yes"/>
                        <breadcrumbs>
                            <xsl:call-template name="breadcrumbs">
                                <xsl:with-param name="component" select="current()"/>
                            </xsl:call-template>
                        </breadcrumbs>
                        <xsl:value-of select="']]&gt;'" disable-output-escaping="yes"/>
                    </field>
                </doc>
            </add>
        </xsl:result-document>
    </xsl:template>

    <xsl:template name="breadcrumbs">
        <xsl:param name="component" required="yes"/>
        <xsl:variable name="parent" select="$component/.."/>
        <xsl:choose>
            <xsl:when test="name($parent) = 'dsc'">
                <xsl:variable name="title">
                    <xsl:for-each select="//ead:ead/ead:archdesc/ead:did/ead:unittitle[1]//text()">
                        <xsl:value-of select="current()"/>
                    </xsl:for-each>
                </xsl:variable>
                <ancestor>
                    <id>
                        <xsl:value-of
                            select="translate(/ead:ead/ead:archdesc/ead:did/ead:unitid, ' ', '')"/>
                    </id>
                    <title>
                        <xsl:value-of select="normalize-space($title)"/>
                    </title>
                </ancestor>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="breadcrumbs">
                    <xsl:with-param name="component" select="$parent"/>
                </xsl:call-template>
                <xsl:variable name="title">
                    <xsl:for-each select="$parent/ead:did/ead:unittitle[1]//text()">
                        <xsl:value-of select="current()"/>
                    </xsl:for-each>
                </xsl:variable>

                <ancestor>
                    <id>
                        <xsl:value-of select="$parent/@id"/>
                    </id>
                    <title>
                        <xsl:value-of select="normalize-space($title)"/>
                    </title>
                </ancestor>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- The raw-excluding-nested-components mode copies everything except for contained components. -->
    <!-- The "raw" mode is just like the raw-excluding-nested-components except that if the first element it matches is a component it's included. -->
    <xsl:template match="attribute() | element() | text() | comment() | processing-instruction()"
        priority="-1" mode="raw">
        <xsl:copy>
            <xsl:apply-templates
                select="attribute() | element() | text() | comment() | processing-instruction()"
                mode="raw-excluding-nested-components"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="attribute() | element() | text() | comment() | processing-instruction()"
        priority="-1" mode="raw-excluding-nested-components">
        <xsl:copy>
            <xsl:apply-templates
                select="attribute() | element() | text() | comment() | processing-instruction()"
                mode="raw-excluding-nested-components"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="ead:c | node()[starts-with(name(), 'c0')]"
        mode="raw-excluding-nested-components"/>

    <!-- the hierarchy mode generatres a hierarchy summary -->
    <xsl:template match="attribute() | element() | text() | comment() | processing-instruction()"
        priority="-1" mode="hierarchy">
        <xsl:apply-templates
            select="attribute() | element() | text() | comment() | processing-instruction()"
            mode="hierarchy"/>
    </xsl:template>
    <xsl:template match="ead:ead" mode="hierarchy">
        <collection>
            <xsl:variable name="shorttitle">
                <xsl:apply-templates select="ead:archdesc/ead:did/ead:unittitle" mode="shorttitle"/>
            </xsl:variable>
            <xsl:variable name="title">
                <xsl:apply-templates select="ead:archdesc/ead:did/ead:unittitle" mode="title"/>
            </xsl:variable>
            <title>
                <xsl:value-of select="normalize-space($title)"/>
            </title>
            <shorttitle>
                <xsl:value-of select="normalize-space($shorttitle)"/>
            </shorttitle>
            <xsl:variable name="components">
                <xsl:apply-templates select="*" mode="hierarchy"/>
            </xsl:variable>
            <component_count>
                <xsl:value-of select="count($components/component)"/>
            </component_count>
            <xsl:copy-of select="$components"/>

        </collection>
    </xsl:template>
    <xsl:template match="ead:c | node()[starts-with(name(), 'c0')]" mode="hierarchy">
        <component>
            <id>
                <xsl:call-template name="getId">
                    <xsl:with-param name="component" select="."/>
                </xsl:call-template>
            </id>
            <xsl:variable name="unittitle">
                <xsl:apply-templates select="ead:did/ead:unittitle" mode="title"/>
            </xsl:variable>
            <xsl:variable name="shortunittitle">
                <xsl:apply-templates select="ead:did/ead:unittitle" mode="shorttitle"/>
            </xsl:variable>
            <type>
                <xsl:value-of select="@level"/>
            </type>
            <unittitle>
                <xsl:value-of select="normalize-space($unittitle)"/>
            </unittitle>
            <shortunittitle>
                <xsl:value-of select="normalize-space($shortunittitle)"/>
            </shortunittitle>
            <xsl:variable name="components">
                <xsl:apply-templates select="*" mode="hierarchy"/>
            </xsl:variable>
            <component_count>
                <xsl:value-of select="count($components/component)"/>
            </component_count>
            <xsl:copy-of select="$components"/>
        </component>
    </xsl:template>

    <xsl:template name="getId">
        <xsl:param name="component" required="yes"/>
        <xsl:choose>
            <xsl:when test="uva:pid">
                <xsl:value-of select="uva:pid"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@id"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="addFieldsFromMods">
        <xsl:param name="mods" required="yes"/>
        <xsl:param name="useRightsString"/>
        <xsl:comment>Fields from MODS BEGIN -----</xsl:comment>
        <!-- The "right_wrapper" feature indicates that we should provide page-level links
					 that include rights information and specifically that that rights information will be
					 stored in the "rights_wrapper_display" field, and a service URL will be included in the
					 rights_wrapper_url_display field to which just the page pid should be appended.
			     -->
        <field name="feature_facet">dl_metadata</field>
        <xsl:variable name="citation">
            <xsl:variable name="title">
                <xsl:choose>
                    <xsl:when test="$mods/mods:titleInfo/mods:title[@type = 'uniform']">
                        <xsl:value-of
                            select="$mods/mods:titleInfo/mods:title[@type = 'uniform'][1]/text()"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$mods/mods:titleInfo/mods:title[1]/text()"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:value-of select="normalize-space($title)"/>
            <xsl:variable name="callNumber"
                select="normalize-space($mods/mods:classification[1]/text())"/>
            <xsl:if test="$callNumber != ''">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="$callNumber"/>
            </xsl:if>
            <xsl:variable name="location">
                <xsl:choose>
                    <xsl:when test="$mods/mods:location/mods:physicalLocation">
                        <xsl:value-of
                            select="normalize-space(($mods/mods:location/mods:physicalLocation)[1])"
                        />
                    </xsl:when>
                    <xsl:when
                        test="$mods/mods:location/mods:holdingSimple/mods:copyInformation[mods:subLocation/text() = 'SPEC-COLL']">
                        <xsl:text>Special Collections, University of Virginia Library, Charlottesville, Va.</xsl:text>
                    </xsl:when>
                </xsl:choose>
            </xsl:variable>
            <xsl:if test="$location != ''">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="$location"/>
            </xsl:if>
        </xsl:variable>

        <field name="rights_wrapper_display">
            <xsl:value-of select="$citation"/>
            <xsl:value-of select="$newline"/>
            <xsl:choose>
                <xsl:when test="$useRightsString = 'In Copyright'">
                    <xsl:text>The UVA Library has determined that this work is in-copyright.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>This single copy was produced for purposes of private study, scholarship, or research, pursuant to the library's rights under the Copyright Act.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>Copyright and other restrictions may apply to any further use of this image.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>See the full Virgo Terms of Use at http://search.lib.virginia.edu/terms.html for more information.</xsl:text>
                </xsl:when>
                <xsl:when test="$useRightsString = 'No Known Copyright'">
                    <xsl:text>The UVA Library is not aware of any copyright interest in this work.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>This single copy was produced for purposes of private study, scholarship, or research. You are responsible for making a rights determination for your own uses.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>See the full Virgo Terms of Use at http://search.lib.virginia.edu/terms.html for more information.</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <!-- Default to Copyright Not Evaluated -->
                    <xsl:text>The UVA Library has not evaluated the copyright status of this work.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>This single copy was produced for purposes of private study, scholarship, or research, pursuant to the library's rights under the Copyright Act.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>Copyright and other restrictions may apply to any further use of this image.</xsl:text>
                    <xsl:value-of select="$newline"/>
                    <xsl:text>See the full Virgo Terms of Use at http://search.lib.virginia.edu/terms.html for more information.</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </field>


        <!-- call number -->


        <xsl:for-each select="$mods/mods:identifier">
            <xsl:if test="$mods/mods:identifier/text() and @type = 'accessionNumber'">
                <field name="call_number_display">
                    <xsl:value-of select="current()"/>
                </field>
                <field name="call_number_text">
                    <xsl:value-of select="current()"/>
                </field>
            </xsl:if>
        </xsl:for-each>


        <!-- title -->
        <xsl:for-each select="$mods/mods:title">
            <xsl:if test="position() = 1 or @type = 'uniform'">
                <field name="main_title_display">
                    <xsl:value-of select="current()"/>
                </field>
                <!-- there can be only one! title facet will BREAK solr sorting if multiple values found -->
                <field name="title_facet">
                    <xsl:value-of select="current()"/>
                </field>
                <field name="title_text">
                    <xsl:value-of select="current()"/>
                </field>
                <field name="title_display">
                    <xsl:value-of select="current()"/>
                </field>
                <field name="title_sort_facet">
                    <xsl:value-of select="translate(current(), $uppercase, $lowercase)"/>
                </field>
            </xsl:if>
        </xsl:for-each>

        <!-- searchable legacy identifier -->

        <xsl:for-each
            select="$mods//mods:identifier[(@displayLabel = 'Negative Number' or @displayLabel = 'Prints Number' or @displayLabel = 'Originating Collection' or @displayLabel = 'Artifact Number' or @displayLabel = 'retrieval ID')]">
            <field name="media_retrieval_id_display">
                <xsl:value-of select="current()"/>
            </field>
            <field name="media_retrieval_id_facet">
                <xsl:value-of select="current()"/>
            </field>
            <field name="media_retrieval_id_text">
                <xsl:value-of select="current()"/>
            </field>
        </xsl:for-each>

        <!-- SOLR can take only one year_multisort_i field, so we need to choose which mods element to utilize -->
        <xsl:for-each select="$mods/mods:originInfo[1]">
            <xsl:choose>
                <xsl:when test="current()/mods:dateIssued[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node"
                            select="current()/mods:dateIssued[@keyDate = 'yes'][1]"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="current()/mods:dateCreated[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node" select="current()/mods:dateCreated"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="current()/mods:dateCaptured[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node"
                            select="current()/mods:dateCaptured[@keyDate = 'yes'][1]"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="current()/mods:dateValid[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node"
                            select="current()/mods:dateValid[@keyDate = 'yes'][1]"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="current()/mods:copyrightDate[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node"
                            select="current()/mods:copyrightDate[@keyDate = 'yes'][1]"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="current()/mods:dateOther[@keyDate = 'yes'][1]">
                    <xsl:call-template name="build-dates">
                        <xsl:with-param name="date-node"
                            select="current()/mods:dateOther[@keyDate = 'yes'][1]"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:for-each>

        <!-- subject text -->
        <xsl:for-each select="$mods//mods:subject">
            <xsl:variable name="text-content">
                <xsl:for-each select="./descendant::text()[matches(., '[\w]+')]">
                    <xsl:if test="matches(current(), '[\w]+')">
                        <!-- add double dash to all trailing subfields -->
                        <xsl:if test="position() != 1">
                            <xsl:text> -- </xsl:text>
                        </xsl:if>
                        <xsl:copy-of select="normalize-space(current())"/>
                    </xsl:if>

                </xsl:for-each>
            </xsl:variable>

            <xsl:choose>
                <xsl:when test="matches($text-content, '[\w]+')">
                    <field name="subject_text">
                        <xsl:value-of select="$text-content"/>
                    </field>
                    <field name="subject_facet">
                        <xsl:value-of select="$text-content"/>
                    </field>
                    <field name="subject_genre_facet">
                        <xsl:value-of select="$text-content"/>
                    </field>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:for-each>

        <!-- place -->
        <xsl:for-each
            select="$mods//mods:place/mods:placeTerm[not(@authority = 'marccountry')]/text()">
            <xsl:choose>
                <xsl:when test="current() = ''"/>
                <xsl:otherwise>
                    <field name="region_facet">
                        <xsl:value-of select="current()"/>
                    </field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

        <!-- library facet -->
        <xsl:for-each
            select="$mods/mods:location/mods:physicalLocation[not(@authority = 'oclcorg')]">
            <xsl:if test="current()/text() != ' '">
                <field name="location_display">
                    <xsl:value-of select="current()/text()"/>
                </field>
            </xsl:if>
            <xsl:variable name="normalizedLibraryName">
                <xsl:choose>
                    <xsl:when
                        test="./text() = 'Special Collections, University of Virginia Library, Charlottesville, Va.'"
                        >Special Collections</xsl:when>

                    <xsl:when
                        test="./text()='Historical Collections &amp; Services, Claude Moore Health Sciences Library, Charlottesville, Va.'"
                        >Health Sciences</xsl:when>
                    <xsl:when
                        test="./text() = 'Special Collections, Arthur J. Morris Law Library, Charlottesville, Va.'"
                        >Law School</xsl:when>
                </xsl:choose>
            </xsl:variable>
            <xsl:if test="$normalizedLibraryName != ''">
                <field name="library_facet">
                    <xsl:value-of select="$normalizedLibraryName"/>
                </field>
            </xsl:if>
        </xsl:for-each>

        <!-- shelf location added for Bicentennial collection 9/16-->

        <xsl:for-each select="$mods/mods:location/mods:shelfLocator">
            <xsl:if test="current()/text() != ' '">
                <field name="location_display">
                    <xsl:value-of select="current()/text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>

        <!-- creator -->

        <!-- added corporate info to accommodate Online Artifacts records -->

        <xsl:if test="$mods/mods:name[@type = 'corporate']">
            <xsl:variable name="corpName">
                <xsl:value-of select="$mods/mods:name[1][@type = 'corporate']/mods:namePart/text()"
                />
            </xsl:variable>
            <xsl:if test="normalize-space($corpName) != ''">
                <field name="author_display">
                    <xsl:value-of select="$corpName"/>
                </field>
                <field name="author_facet">
                    <xsl:value-of select="$corpName"/>
                </field>
            </xsl:if>
        </xsl:if>

        <xsl:for-each select="$mods/mods:name[@type = 'personal']">
            <xsl:variable name="fname">
                <xsl:choose>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'family'] and current()/mods:namePart[@type = 'family'][substring-before(., ',') != '']"
                        >sd <xsl:value-of
                            select="substring-before(current()/mods:namePart[@type = 'family'], ',')"
                        />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="current()/mods:namePart[@type = 'family']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="gname">
                <xsl:choose>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'given'] and current()/mods:namePart[@type = 'given'][substring-before(., ',') != '']">
                        <xsl:value-of
                            select="substring-before(current()/mods:namePart[@type = 'given'], ',')"
                        />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="current()/mods:namePart[@type = 'given']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="term-of-address">
                <xsl:choose>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'termsOfAddress'] and current()/mods:namePart[@type = 'termsOfAddress'][substring-before(., ',') != '']">
                        <xsl:value-of
                            select="substring-before(current()/mods:namePart[@type = 'termsOfAddress'], ',')"
                        />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="current()/mods:namePart[@type = 'termsOfAddress']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="nameFull">
                <xsl:choose>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'family'] and current()/mods:namePart[@type = 'given']">
                        <xsl:value-of select="$fname"/>
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="$gname"/>
                    </xsl:when>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'family'] and current()/mods:namePart[@type = 'termsOfAddress']">
                        <xsl:value-of select="$fname"/>
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="$term-of-address"/>
                    </xsl:when>
                    <xsl:when
                        test="current()/mods:namePart[@type = 'given'] and current()/mods:namePart[@type = 'termsOfAddress']">
                        <xsl:value-of select="$gname"/>
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="$term-of-address"/>
                    </xsl:when>
                    <xsl:when
                        test="contains(current()/mods:namePart[not(@type = 'date')][not(@type = 'termsOfAddress')][1], ',') and count(current()/mods:namePart) = 1">
                        <xsl:value-of select="current()/mods:namePart[1]"/>
                    </xsl:when>
                    <xsl:when test="current()/mods:namePart[not(@type = 'date')]">
                        <xsl:for-each select="current()/mods:namePart[not(@type = 'date')]">
                            <xsl:choose>
                                <xsl:when test="contains(., ',') and substring-after(., ',') = ''">
                                    <xsl:value-of select="substring-before(., ',')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="current()"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:if test="normalize-space($nameFull) != ''">
                <field name="author_facet">
                    <xsl:value-of select="$nameFull"/>
                </field>
                <xsl:choose>
                    <xsl:when test="child::mods:namePart[@type = 'date']">
                        <field name="author_display"><xsl:value-of select="$nameFull"/>,
                                <xsl:value-of select="child::mods:namePart[@type = 'date']/text()"/>
                        </field>
                    </xsl:when>
                    <xsl:otherwise>
                        <field name="author_display">
                            <xsl:value-of select="$nameFull"/>
                        </field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>

            <xsl:choose>
                <xsl:when test="position() = 1 and child::mods:namePart[@type = 'date']">
                    <field name="author_sort_facet">
                        <xsl:value-of select="translate($nameFull, $uppercase, $lowercase)"/>
                        <xsl:text> </xsl:text>
                        <xsl:value-of
                            select="translate(child::mods:namePart[@type = 'date']/text(), $uppercase, $lowercase)"
                        />
                    </field>
                </xsl:when>
                <xsl:when test="position() = 1">
                    <field name="author_sort_facet">
                        <xsl:value-of select="translate($nameFull, $uppercase, $lowercase)"/>
                    </field>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:for-each>

        <!-- Publication information added for Bicentennial collection 9/2016-->

        <xsl:for-each select="$mods/mods:originInfo/mods:publisher">
            <xsl:choose>
                <xsl:when test="./text()">
                    <field name="published_display">
                        <xsl:value-of select="text()"/>
                    </field>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>

        <!-- series facet -->
        <xsl:for-each
            select="$mods/mods:relatedItem[@type = 'series'][not(@displayLabel = 'Part of')]">
            <xsl:variable name="dateRange" xml:space="default">
                <xsl:choose>
                    <xsl:when
                        test="
                            $mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'start'] and
                            $mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'end']"
                        >, <xsl:value-of
                            select="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'start']"
                        /> - <xsl:value-of
                            select="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'end']"
                        /></xsl:when>
                    <xsl:when
                        test="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'start']"
                        >, <xsl:value-of
                            select="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'start']"
                        /> - ?</xsl:when>
                    <xsl:when
                        test="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'end']"
                        >, ? - <xsl:value-of
                            select="$mods/mods:relatedItem[1]/mods:originInfo[1]/mods:dateCreated[@point = 'end']"
                        />)</xsl:when>
                    <xsl:otherwise/>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="volume" xml:space="default">
                <xsl:choose>
                    <xsl:when test="current()/mods:part/mods:detail[@type = 'volume']">
                        <xsl:value-of
                            select="current()/mods:part/mods:detail[@type = 'volume']/mods:number"/>
                    </xsl:when>
                    <xsl:otherwise/>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="issue" xml:space="default">
                <xsl:choose>
                    <xsl:when test="current()/mods:part/mods:detail[@type = 'issue']">
                        <xsl:value-of
                            select="current()/mods:part/mods:detail[@type = 'issue']/mods:number"/>
                    </xsl:when>
                    <xsl:otherwise/>
                </xsl:choose>
            </xsl:variable>

            <field name="series_title_text">
                <xsl:for-each select="current()/mods:titleInfo/descendant::*">
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </field>
            <field name="series_title_facet">
                <xsl:for-each
                    select="current()/mods:titleInfo/descendant::*[local-name() != 'nonSort']">
                    <xsl:value-of select="."/>
                    <xsl:if test="position() != last()">
                        <xsl:text> </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </field>
            <field name="series_title_display">
                <xsl:for-each
                    select="current()/mods:titleInfo/descendant::*[local-name() != 'nonSort']">
                    <xsl:value-of select="."/>
                    <xsl:if test="$dateRange">
                        <xsl:value-of xml:space="default" select="$dateRange"/>
                    </xsl:if>
                    <xsl:if test="$volume != ''">
                        <xsl:text>, Volume </xsl:text>
                        <xsl:value-of xml:space="default" select="$volume"/>
                    </xsl:if>
                    <xsl:if test="$issue != ''">
                        <xsl:text>, Issue </xsl:text>
                        <xsl:value-of xml:space="default" select="$issue"/>
                    </xsl:if>
                    <xsl:if test="position() != last()">
                        <xsl:text> </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </field>
        </xsl:for-each>

        <!-- date (range) -->
        <xsl:for-each select="$mods/mods:relatedItem[1]/mods:originInfo[1]">
            <field name="startDate_text">
                <xsl:value-of select="current()/mods:dateCreated[@point = 'start']"/>
            </field>
            <field name="endDate_text">
                <xsl:value-of select="current()/mods:dateCreated[@point = 'end']"/>
            </field>
        </xsl:for-each>

        <!-- format facet -->
        <field name="format_facet">Online</field>
        <field name="format_text">Online</field>
        <xsl:for-each select="$mods/mods:typeOfResource">
            <xsl:choose>

                <!-- typeOfResource 'three dimensional object' -> Physical Object -->

                <xsl:when test="./text() = 'three dimensional object'">
                    <field name="format_text">
                        <xsl:value-of>Physical Object</xsl:value-of>
                    </field>
                    <field name="format_facet">
                        <xsl:value-of>Physical Object</xsl:value-of>
                    </field>
                </xsl:when>

                <!-- typeOfResource 'still image' and genre 'Photographs' -> Photograph -->

                <xsl:when
                    test="./text() = 'still image' and $mods/mods:genre/text() = 'Photographs'">
                    <field name="format_text">
                        <xsl:value-of>Photograph</xsl:value-of>
                    </field>
                    <field name="format_facet">
                        <xsl:value-of>Photograph</xsl:value-of>
                    </field>
                </xsl:when>

                <!-- typeOfResource 'still image' and genre 'art reproduction' -> Visual Materials -->

                <xsl:when
                    test="./text() = 'still image' and $mods/mods:genre/text() = 'art reproduction'">
                    <field name="format_text">
                        <xsl:value-of>Visual Materials</xsl:value-of>
                    </field>
                    <field name="format_facet">
                        <xsl:value-of>Visual Materials</xsl:value-of>
                    </field>
                </xsl:when>

                <!-- typeOfResource 'still image' and genre 'caricatures' -> Visual Materials -->

                <xsl:when
                    test="./text() = 'still image' and $mods/mods:genre/text() = 'caricatures'">
                    <field name="format_text">
                        <xsl:value-of>Visual Materials</xsl:value-of>
                    </field>
                    <field name="format_facet">
                        <xsl:value-of>Visual Materials</xsl:value-of>
                    </field>
                </xsl:when>

                <xsl:otherwise>
                    <field name="format_text">
                        <xsl:value-of select="./text()"/>
                    </field>
                    <field name="format_facet">
                        <xsl:value-of select="./text()"/>
                    </field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

        <!-- Collection-specific Facets and Fields -->

        <!-- Vanity Fair -->
        <xsl:if
            test="$mods/mods:relatedItem[@type = 'series'][@displayLabel = 'Part of']/mods:titleInfo/mods:title/text() = 'Cecil Lang Collection of Vanity Fair Illustrations'">
            <field name="has_optional_facet">category_facet</field>
            <field name="has_optional_facet">group_facet</field>
            <field name="has_optional_facet">signature_facet</field>
            <xsl:if test="$mods/mods:note[@displayLabel = 'Category']/text()">
                <field name="category_facet">
                    <xsl:value-of select="$mods/mods:note[@displayLabel = 'Category']"/>
                </field>
                <field name="category_display">
                    <xsl:value-of select="$mods/mods:note[@displayLabel = 'Category']"/>
                </field>
            </xsl:if>
            <xsl:if test="$mods/mods:note[@displayLabel = 'Group']/text()">
                <field name="group_facet">
                    <xsl:value-of
                        select="$mods/mods:note[@displayLabel = 'Group']/substring-before(., ',')"/>
                </field>
                <field name="group_display">
                    <xsl:value-of select="$mods/mods:note[@displayLabel = 'Group']"/>
                </field>
            </xsl:if>
            <xsl:if test="$mods/mods:note[@displayLabel = 'Signature']/text()">
                <field name="signature_facet">
                    <xsl:value-of select="$mods/mods:note[@displayLabel = 'Signature']"/>
                </field>
                <field name="signature_display">
                    <xsl:value-of select="$mods/mods:note[@displayLabel = 'Signature']"/>
                </field>
            </xsl:if>
        </xsl:if>

        <!-- End of Collection-specific Facets and Fields -->

        <!-- genre -->
        <xsl:for-each select="$mods/mods:genre/text()">
            <field name="genre_text">
                <xsl:value-of select="current()"/>
            </field>
            <field name="genre_facet">
                <xsl:value-of select="current()"/>
            </field>
        </xsl:for-each>

        <!-- physical description -->

        <xsl:choose>
            <xsl:when test="$mods/mods:abstract/text()">
                <xsl:variable name="descriptionDisplay">
                    <xsl:value-of select="$mods/mods:abstract"/>
                </xsl:variable>
                <xsl:if test="$descriptionDisplay">
                    <field name="media_description_display">
                        <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                    </field>
                    <field name="desc_meta_file_display">
                        <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                    </field>
                    <field name="media_description_text">
                        <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                    </field>
                </xsl:if>
            </xsl:when>

            <xsl:otherwise>
                <xsl:for-each select="$mods/mods:physicalDescription">
                    <xsl:variable name="descriptionDisplay">
                        <xsl:for-each select="current()/child::*">
                            <xsl:choose>
                                <xsl:when test="local-name() = 'form'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when
                                    test="
                                        local-name() = 'note' and ./@displayLabel = 'condition' and not(matches(text(),
                                        '^\s+$'))">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when
                                    test="local-name() = 'note' and ./@displayLabel = 'size inches'">
                                    <xsl:text xml:space="default">Plate size: </xsl:text>
                                    <xsl:value-of select="."/>
                                    <xsl:text xml:space="default"> inches; </xsl:text>
                                </xsl:when>
                                <xsl:otherwise/>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:variable>

                    <xsl:if test="$descriptionDisplay">
                        <field name="media_description_display">
                            <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                        </field>
                        <field name="desc_meta_file_display">
                            <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                        </field>
                        <field name="media_description_text">
                            <xsl:value-of select="normalize-space($descriptionDisplay)"/>
                        </field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>


        <!-- staff note -->
        <xsl:for-each select="$mods/mods:note[@displayLabel = 'staff']">
            <xsl:if test="./text() != ' '">
                <field name="note_text">Staff note: <xsl:value-of select="current()"/></field>
                <!-- use if you want this data to be searchable -->
                <field name="note_display">Staff note: <xsl:value-of select="current()"/></field>
                <!-- use if you want this data to be available for display in blacklight brief or full record -->
            </xsl:if>
        </xsl:for-each>

        <!-- use and access (rough version) -->
        <xsl:for-each select="$mods//mods:accessCondition[@type = 'restrictionOnAccess']">
            <xsl:variable name="accessRestriction">
                <!-- If access restriction element is empty, we will restrict object by default -->
                <xsl:choose>
                    <xsl:when test="current()/text()">
                        <xsl:value-of select="normalize-space(current())"/>
                    </xsl:when>
                    <xsl:otherwise>RESTRICTED</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <field name="access_display"><xsl:value-of select="current()/@displayLabel"/>:
                    <xsl:value-of select="$accessRestriction"/></field>
            <field name="access_text">
                <xsl:value-of select="$accessRestriction"/>
            </field>
        </xsl:for-each>
        <xsl:for-each select="$mods//mods:accessCondition[@type = 'useAndReproduction']">
            <xsl:variable name="accessUse">
                <!-- If use restriction element is empty, we will restrict object by default -->
                <xsl:choose>
                    <xsl:when test="current()/text()">
                        <xsl:value-of select="normalize-space(current())"/>
                    </xsl:when>
                    <xsl:otherwise>RESTRICTED</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <field name="access_display"><xsl:value-of select="current()/@displayLabel"/>:
                    <xsl:value-of select="$accessUse"/></field>
            <field name="access_text">
                <xsl:value-of select="$accessUse"/>
            </field>
        </xsl:for-each>
        <xsl:comment>Fields from MODS END -----</xsl:comment>
    </xsl:template>

    <xsl:template name="build-dates">
        <xsl:param name="date-node" select="'No node sent to template build-dates'"/>
        <xsl:for-each select="$date-node[1]">
            <xsl:choose>
                <xsl:when test="matches(., '^\d{4}')">
                    <xsl:variable name="yearOnly">
                        <xsl:value-of select="substring(., 1, 4)"/>
                    </xsl:variable>
                    <!--		<field name="year_display">
					<xsl:value-of select="."/>
					</field>-->
                    <!-- there can be only one year_multisort_i in a Solr record -->
                    <xsl:if test="current()[@keyDate = 'yes']">
                        <field name="year_multisort_i">
                            <xsl:value-of select="$yearOnly"/>
                        </field>
                    </xsl:if>
                    <!-- if dates available form a range, process elsewhere -->
                    <xsl:choose>
                        <xsl:when test="current()[@point = 'start'] and $date-node[@point = 'end']">
                            <xsl:call-template name="build-daterange">
                                <xsl:with-param name="date-pair"
                                    select="$date-node[@point = 'start' or @point = 'end']"/>
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                            <field name="date_text">
                                <xsl:value-of select="."/>
                            </field>
                            <field name="year_display">
                                <xsl:if test="current()[@qualifier = 'approximate']">
                                    <xsl:text>circa </xsl:text>
                                </xsl:if>
                                <xsl:value-of select="."/>
                            </field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="./text() = 'Unknown Date' or ./text() = 'Unknown date'">
                    <field name="published_date_display">
                        <xsl:value-of select="."/>
                    </field>
                </xsl:when>
                <xsl:otherwise>
                    <field name="published_date_display">
                        <xsl:value-of select="."/>
                    </field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="build-daterange">
        <xsl:param name="date-pair" select="'No nodes sent to template build-daterange'"/>
        <xsl:variable name="range-text">
            <xsl:value-of select="$date-pair[@point = 'start']"/> - <xsl:value-of
                select="$date-pair[@point = 'end']"/>
        </xsl:variable>
        <field name="year_display">
            <xsl:if test="$date-pair[@point = 'start'][@qualifier = 'approximate']">
                <xsl:text>circa </xsl:text>
            </xsl:if>
            <xsl:value-of select="$range-text"/>
        </field>
        <field name="date_text">
            <xsl:value-of select="$range-text"/>
        </field>
    </xsl:template>

</xsl:stylesheet>
