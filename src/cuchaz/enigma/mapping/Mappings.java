/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Maps;

import cuchaz.enigma.Util;
import cuchaz.enigma.analysis.Ancestries;
import cuchaz.enigma.analysis.DeobfuscatedAncestries;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class Mappings implements Serializable
{
	private static final long serialVersionUID = 4649790259460259026L;
	
	protected Map<String,ClassMapping> m_classesByObf;
	protected Map<String,ClassMapping> m_classesByDeobf;
	
	public Mappings( )
	{
		m_classesByObf = Maps.newHashMap();
		m_classesByDeobf = Maps.newHashMap();
	}
	
	public Mappings( Iterable<ClassMapping> classes )
	{
		this();
		
		for( ClassMapping classMapping : classes )
		{
			m_classesByObf.put( classMapping.getObfName(), classMapping );
			m_classesByDeobf.put( classMapping.getDeobfName(), classMapping );
		}
	}
	
	public static Mappings newFromResource( String resource )
	throws IOException
	{
		InputStream in = null;
		try
		{
			in = Mappings.class.getResourceAsStream( resource );
			return newFromStream( in );
		}
		finally
		{
			Util.closeQuietly( in );
		}
	}
	
	public Collection<ClassMapping> classes( )
	{
		assert( m_classesByObf.size() == m_classesByDeobf.size() );
		return m_classesByObf.values();
	}
	
	protected void addClassMapping( ClassMapping classMapping )
	{
		if( m_classesByObf.containsKey( classMapping.getObfName() ) )
		{
			throw new Error( "Already have mapping for " + classMapping.getObfName() );
		}
		if( m_classesByDeobf.containsKey( classMapping.getDeobfName() ) )
		{
			throw new Error( "Already have mapping for " + classMapping.getDeobfName() );
		}
		m_classesByObf.put( classMapping.getObfName(), classMapping );
		m_classesByDeobf.put( classMapping.getDeobfName(), classMapping );
		assert( m_classesByObf.size() == m_classesByDeobf.size() );
	}
	
	public ClassMapping getClassByObf( ClassEntry entry )
	{
		return getClassByObf( entry.getName() );
	}
	
	public ClassMapping getClassByObf( String obfName )
	{
		return m_classesByObf.get( obfName );
	}
	
	public ClassMapping getClassByDeobf( ClassEntry entry )
	{
		return getClassByObf( entry.getName() );
	}
	
	public ClassMapping getClassByDeobf( String deobfName )
	{
		return m_classesByDeobf.get( deobfName );
	}
	
	public Translator getTranslator( Ancestries ancestries, TranslationDirection direction )
	{
		return new Translator(
			direction,
			direction.choose( m_classesByObf, m_classesByDeobf ),
			direction.choose( ancestries, new DeobfuscatedAncestries( ancestries, m_classesByObf, m_classesByDeobf ) )
		);
	}
	
	public static Mappings newFromStream( InputStream in )
	throws IOException
	{
		try
		{
			return (Mappings)new ObjectInputStream( new GZIPInputStream( in ) ).readObject();
		}
		catch( ClassNotFoundException ex )
		{
			throw new Error( ex );
		}
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		for( ClassMapping classMapping : m_classesByObf.values() )
		{
			buf.append( classMapping.toString() );
			buf.append( "\n" );
		}
		return buf.toString();
	}
	
	public void renameObfClass( String oldObfName, String newObfName )
	{
		for( ClassMapping classMapping : new ArrayList<ClassMapping>( classes() ) )
		{
			if( classMapping.renameObfClass( oldObfName, newObfName ) )
			{
				m_classesByObf.remove( oldObfName );
				m_classesByObf.put( newObfName, classMapping );
				assert( m_classesByObf.size() == m_classesByDeobf.size() );
			}
		}
	}
	
	public Set<String> getAllObfClassNames( )
	{
		final Set<String> classNames = Sets.newHashSet();
		for( ClassMapping classMapping : classes() )
		{
			// add the class name
			classNames.add( classMapping.getObfName() );
			
			// add classes from method signatures
			for( MethodMapping methodMapping : classMapping.methods() )
			{
				SignatureUpdater.update( methodMapping.getObfSignature(), new ClassNameUpdater( )
				{
					@Override
					public String update( String className )
					{
						classNames.add( className );
						return className;
					}
				} );
			}
		}
		return classNames;
	}

	public boolean containsDeobfClass( String deobfName )
	{
		return m_classesByDeobf.containsKey( deobfName );
	}
	
	public boolean containsDeobfField( ClassEntry obfClassEntry, String deobfName )
	{
		ClassMapping classMapping = m_classesByObf.get( obfClassEntry.getName() );
		if( classMapping != null )
		{
			return classMapping.containsDeobfField( deobfName );
		}
		return false;
	}

	public boolean containsDeobfMethod( ClassEntry obfClassEntry, String deobfName, String deobfSignature )
	{
		ClassMapping classMapping = m_classesByObf.get( obfClassEntry.getName() );
		if( classMapping != null )
		{
			return classMapping.containsDeobfMethod( deobfName, deobfSignature );
		}
		return false;
	}

	public boolean containsArgument( MethodEntry obfMethodEntry, String name )
	{
		ClassMapping classMapping = m_classesByObf.get( obfMethodEntry.getClassName() );
		if( classMapping != null )
		{
			return classMapping.containsArgument( obfMethodEntry, name );
		}
		return false;
	}
}
