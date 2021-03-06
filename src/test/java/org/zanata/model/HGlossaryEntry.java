/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.model;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Entity
@Setter
@EqualsAndHashCode(callSuper = true, doNotUseGetters = true, exclude = "glossaryTerms")
@ToString(of = {"sourceRef", "srcLocale"})
public class HGlossaryEntry extends ModelEntityBase {
    private static final long serialVersionUID = -4200183325180630061L;

    private Map<HLocale, HGlossaryTerm> glossaryTerms;
    private String sourceRef;
    private HLocale srcLocale;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "glossaryEntry")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @MapKey(name = "locale")
    public Map<HLocale, HGlossaryTerm> getGlossaryTerms() {
        if (glossaryTerms == null) {
            glossaryTerms = new HashMap<HLocale, HGlossaryTerm>();
        }
        return glossaryTerms;
    }

    @Type(type = "text")
    public String getSourceRef() {
        return sourceRef;
    }

    @OneToOne
    @JoinColumn(name = "srcLocaleId", nullable = false)
    public HLocale getSrcLocale() {
        return srcLocale;
    }

}
