<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">
    
    <!-- To map a field, if it's simply renaming it, add it to this list.   Otherwise, add templates to handle it below.  To drop a field, stimply omit from either place -->
    <xsl:variable name="fieldMap">
        <map v3="id" v4="id"/>
        <map v3="part_pid_display" v4="identifier_e_stored" />
        <map v3="shadowed_location_facet" v4="shadowed_location_f"/>
        <map v3="title_display" v4="title_tsearch_stored"/>
        <map v3="author_facet" v4="author_tsearchf_stored"/>
        <map v3="author_added_entry_text" v4="author_added_entry_tsearchf_stored"/>
        <map v3="thumbnail_url_display" v4="thumbnail_url_stored" />
        <map v3="source_facet" v4="source_f_stored"/>
        <map v3="format_facet" v4="format_f_stored"/>
        <map v3="library_facet" v4="library_f_stored"/>
        <map v3="subject_facet" v4="subject_tsearchf_stored"/>
        <map v3="extent_display" v4="extent_tsearch_stored"/>
        <map v3="issued_date_display" v4="published_display_tsearch_stored"/>
        <map v3="note_display" v4="note_tsearch_stored"/>
        <map v3="rs_uri_display" v4="rs_uri_a" />
        <map v3="duration_display" v4="video_run_time_stored" />
        <map v3="genre_display" v4="topic_form_genre_tsearch_stored" />
        <map v3="publisher_display" v4="publisher_name_tsearch_stored" />
        <map v3="digital_collection_facet" v4="collection_f" />
        <map v3="language_facet" v4="language_f" />
    </xsl:variable>
    
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
    <xsl:template match="add">
        <add>
            <xsl:apply-templates select="*"/>
        </add>
    </xsl:template>
    
    <xsl:template match="doc">
        <doc>
            <field name="circulating_f">true</field>
            <field name="source_f_stored">Avalon</field>
            <field name="uva_availability_f_stored">Online</field>
            <field name="anon_availability_f_stored">Online</field>
            <field name="record_date_stored">
                <xsl:value-of select="current-dateTime()"/>
            </field>
            <xsl:variable name="audio" select="field[@name = 'format_facet'][text() = 'Streaming Audio']"/>
            <xsl:variable name="video" select="field[@name = 'format_facet'][text() = 'Online Video']"/>
            <xsl:variable name="solrId" select="field[@name = 'id']/text()" />
            <xsl:variable name="avalonId">
                <xsl:variable name="idsAreTheSame" select="matches($solrId, '^avalon:\d+$')"/>
                <xsl:if test="$idsAreTheSame"><xsl:value-of select="$solrId"/></xsl:if>
                <xsl:if test="not($idsAreTheSame)"><xsl:value-of select="substring($solrId, 8)"/></xsl:if>
            </xsl:variable>
            <field name="url_str_stored">https://avalon.lib.virginia.edu/media_objects/<xsl:value-of select="$avalonId"/></field>
            <field name="data_source_str_stored">avalon</field>
            <xsl:if test="$audio and not($video)">
                <field name="url_label_str_stored">Listen Online</field>
                <field name="pool_f">music_recordings</field>
                <field name="work_title3_key_sort"><xsl:value-of select="concat(translate(normalize-space(field[@name = 'title_sort_facet']/text()), ' &quot;', '_'), '//MusicRecording')" /></field>
                <field name="work_title2_key_sort"><xsl:value-of select="concat(translate(normalize-space(field[@name = 'title_sort_facet']/text()), ' &quot;', '_'), '/', translate(normalize-space(field[@name = 'author_facet'][1]/text()), ' ', '_'), '/MusicRecording')" /></field>
            </xsl:if>
            <xsl:if test="$video">
                <field name="url_label_str_stored">Watch Online</field>
                <field name="pool_f">video</field>
                <field name="work_title3_key_sort"><xsl:value-of select="concat(translate(normalize-space(field[@name = 'title_sort_facet']/text()), ' &quot;', '_'), '//video')" /></field>
                <field name="work_title2_key_sort"><xsl:value-of select="concat(translate(normalize-space(field[@name = 'title_sort_facet']/text()), ' &quot;', '_'), '/', translate(normalize-space(field[@name = 'author_facet'][1]/text()), ' ', '_'), '/video')" /></field>
            </xsl:if>
            <!-- flat_broke_with_children_women_in_the_age_of_welfare_reform//video -->
            <field name="uva_availability_f_stored">On shelf</field>
            <field name="anon_availability_f_stored">On shelf</field>
            <xsl:apply-templates select="*"/>
        </doc>
    </xsl:template>
    
    <xsl:template match="field[@name = 'year_multisort_i']">
        <field name="published_date">
            <xsl:value-of select="concat(text(), '-01-01T00:00:00Z')"/>
        </field>
    </xsl:template>
    
    <xsl:template match="field">
        <xsl:variable name="v3FieldName" select="@name"/>
        <xsl:variable name="mapEntry" select="$fieldMap/map[@v3 = $v3FieldName]"/>
        <xsl:if test="$mapEntry">
            <field>
                <xsl:attribute name="name" select="$mapEntry/@v4"/>
                <xsl:apply-templates select="node()" mode="copy"/>
            </field>
        </xsl:if>
        <xsl:if test="not($mapEntry)">
            <xsl:comment>Dropped unmapped V3 "<xsl:value-of select="$v3FieldName"/>" field.</xsl:comment>
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template match="@* | node()" mode="copy">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    
    <!-- ======================================================================= -->
    <!-- DEFAULT TEMPLATE                                                        -->
    <!-- ======================================================================= -->
    
    
    <xsl:template match="@* | node()">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
    
    <xsl:template match="@* | node()">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
</xsl:stylesheet>
